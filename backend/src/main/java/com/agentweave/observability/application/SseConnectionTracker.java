package com.agentweave.observability.application;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SseConnectionTracker {

    private static final Logger log = LoggerFactory.getLogger(SseConnectionTracker.class);
    private static final String MESSAGE_DELTA_EVENT = "message_delta";
    private static final String TOOL_CALL_STARTED_EVENT = "tool_call_started";
    private static final String TOOL_CALL_FINISHED_EVENT = "tool_call_finished";
    private static final String CITATION_EVENT = "citation";

    private final AgentWeaveMetrics agentWeaveMetrics;
    private final Map<UUID, SseConnectionState> connections = new ConcurrentHashMap<>();

    public SseConnectionTracker(AgentWeaveMetrics agentWeaveMetrics) {
        this.agentWeaveMetrics = agentWeaveMetrics;
    }

    public SseConnectionScope start(
            UUID userId,
            UUID conversationId,
            UUID messageId,
            String traceId,
            String endpoint,
            String modelName) {
        SseConnectionScope scope = new SseConnectionScope(
                userId,
                conversationId,
                messageId,
                normalize(traceId),
                normalize(endpoint),
                System.nanoTime());
        SseConnectionState state = new SseConnectionState(scope, normalize(modelName));
        connections.put(messageId, state);
        agentWeaveMetrics.recordSseConnectionStarted();
        log.info(
                "SSE connection started: traceId={}, userId={}, conversationId={}, messageId={}, endpoint={}, modelName={}",
                scope.traceId(),
                userId,
                conversationId,
                messageId,
                scope.endpoint(),
                state.modelName());
        return scope;
    }

    public void modelResolved(SseConnectionScope scope, String modelName) {
        state(scope).ifPresent(state -> state.modelName.set(normalize(modelName)));
    }

    public void recordEvent(SseConnectionScope scope, String eventType) {
        state(scope).ifPresent(state -> {
            String normalizedEventType = normalize(eventType);
            if (MESSAGE_DELTA_EVENT.equals(normalizedEventType)) {
                state.status.compareAndSet(SseConnectionStatus.CONNECTING, SseConnectionStatus.STREAMING);
                recordFirstTokenIfNeeded(state);
                state.deltaCount.incrementAndGet();
            } else if (TOOL_CALL_STARTED_EVENT.equals(normalizedEventType)) {
                state.status.set(SseConnectionStatus.TOOL_CALLING);
                state.toolEventCount.incrementAndGet();
            } else if (TOOL_CALL_FINISHED_EVENT.equals(normalizedEventType)) {
                state.status.set(SseConnectionStatus.STREAMING);
                state.toolEventCount.incrementAndGet();
            } else if (CITATION_EVENT.equals(normalizedEventType)) {
                state.citationCount.incrementAndGet();
            }
            state.eventCount.incrementAndGet();
            agentWeaveMetrics.recordSseEvent(
                    state.scope.endpoint(),
                    state.modelName(),
                    state.status.get().name(),
                    normalizedEventType);
        });
    }

    public void complete(SseConnectionScope scope) {
        finish(scope, SseConnectionStatus.COMPLETED, null);
    }

    public void fail(SseConnectionScope scope, String errorMessage) {
        finish(scope, SseConnectionStatus.FAILED, errorMessage);
    }

    public void timeout(SseConnectionScope scope, String errorMessage) {
        finish(scope, SseConnectionStatus.TIMEOUT, errorMessage);
    }

    public void cancel(SseConnectionScope scope, String reason) {
        finish(scope, SseConnectionStatus.CANCELLED, reason);
    }

    public void clientDisconnected(SseConnectionScope scope) {
        finish(scope, SseConnectionStatus.CLIENT_DISCONNECTED, "client disconnected");
    }

    public SseConnectionSummary snapshot(SseConnectionScope scope) {
        return state(scope)
                .map(this::summary)
                .orElse(null);
    }

    private java.util.Optional<SseConnectionState> state(SseConnectionScope scope) {
        if (scope == null) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.ofNullable(connections.get(scope.messageId()));
    }

    private void finish(SseConnectionScope scope, SseConnectionStatus status, String errorMessage) {
        SseConnectionState state = state(scope).orElse(null);
        if (state == null || !state.finished.compareAndSet(false, true)) {
            return;
        }
        state.status.set(status);
        long connectionDurationMs = elapsedMillis(state.scope.startedAtNanos());
        agentWeaveMetrics.recordSseConnectionFinished(
                status.name(),
                state.scope.endpoint(),
                state.modelName(),
                connectionDurationMs);
        log.info(
                "SSE connection finished: traceId={}, userId={}, conversationId={}, messageId={}, eventCount={}, "
                        + "deltaCount={}, toolEventCount={}, citationCount={}, firstTokenDurationMs={}, "
                        + "connectionDurationMs={}, status={}, errorMessage={}",
                state.scope.traceId(),
                state.scope.userId(),
                state.scope.conversationId(),
                state.scope.messageId(),
                state.eventCount.get(),
                state.deltaCount.get(),
                state.toolEventCount.get(),
                state.citationCount.get(),
                state.firstTokenDurationMs(),
                connectionDurationMs,
                status.name(),
                sanitize(errorMessage));
        connections.remove(scope.messageId(), state);
    }

    private void recordFirstTokenIfNeeded(SseConnectionState state) {
        long durationMs = elapsedMillis(state.scope.startedAtNanos());
        if (state.firstTokenDurationMs.compareAndSet(-1L, durationMs)) {
            agentWeaveMetrics.recordSseFirstTokenDuration(
                    state.scope.endpoint(),
                    state.modelName(),
                    state.status.get().name(),
                    durationMs);
        }
    }

    private SseConnectionSummary summary(SseConnectionState state) {
        return new SseConnectionSummary(
                state.scope.traceId(),
                state.scope.userId(),
                state.scope.conversationId(),
                state.scope.messageId(),
                state.eventCount.get(),
                state.deltaCount.get(),
                state.toolEventCount.get(),
                state.citationCount.get(),
                state.firstTokenDurationMs(),
                elapsedMillis(state.scope.startedAtNanos()),
                state.status.get(),
                state.modelName());
    }

    private long elapsedMillis(long startedAtNanos) {
        return Duration.ofNanos(System.nanoTime() - startedAtNanos).toMillis();
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value.trim();
    }

    private String sanitize(String errorMessage) {
        if (errorMessage == null || errorMessage.isBlank()) {
            return null;
        }
        String sanitized = errorMessage
                .replaceAll("(?i)(api[-_ ]?key|token|secret|password)=\\S+", "$1=******")
                .trim();
        if (sanitized.length() <= 300) {
            return sanitized;
        }
        return sanitized.substring(0, 300);
    }

    public record SseConnectionScope(
            UUID userId,
            UUID conversationId,
            UUID messageId,
            String traceId,
            String endpoint,
            long startedAtNanos) {
    }

    public record SseConnectionSummary(
            String traceId,
            UUID userId,
            UUID conversationId,
            UUID messageId,
            int eventCount,
            int deltaCount,
            int toolEventCount,
            int citationCount,
            Long firstTokenDurationMs,
            long connectionDurationMs,
            SseConnectionStatus status,
            String modelName) {
    }

    private static final class SseConnectionState {

        private final SseConnectionScope scope;
        private final AtomicReference<String> modelName;
        private final AtomicReference<SseConnectionStatus> status = new AtomicReference<>(SseConnectionStatus.CONNECTING);
        private final AtomicInteger eventCount = new AtomicInteger();
        private final AtomicInteger deltaCount = new AtomicInteger();
        private final AtomicInteger toolEventCount = new AtomicInteger();
        private final AtomicInteger citationCount = new AtomicInteger();
        private final AtomicLong firstTokenDurationMs = new AtomicLong(-1L);
        private final AtomicBoolean finished = new AtomicBoolean();

        private SseConnectionState(SseConnectionScope scope, String modelName) {
            this.scope = scope;
            this.modelName = new AtomicReference<>(modelName);
        }

        private String modelName() {
            return modelName.get();
        }

        private Long firstTokenDurationMs() {
            long durationMs = firstTokenDurationMs.get();
            return durationMs < 0 ? null : durationMs;
        }
    }
}
