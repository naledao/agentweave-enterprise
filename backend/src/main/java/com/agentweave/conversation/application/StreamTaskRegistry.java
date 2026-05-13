package com.agentweave.conversation.application;

import com.agentweave.shared.exception.TooManyRequestsException;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

@Component
public class StreamTaskRegistry {

    private final ChatProperties chatProperties;
    private final Map<StreamTaskKey, StreamTask> tasks = new ConcurrentHashMap<>();

    public StreamTaskRegistry(ChatProperties chatProperties) {
        this.chatProperties = chatProperties;
    }

    public synchronized void assertCanRegister(UUID userId) {
        if (activeStreamCount(userId) >= chatProperties.maxConcurrentStreamsPerUser()) {
            throw new TooManyRequestsException("too many active chat streams");
        }
    }

    public synchronized StreamTask register(
            UUID userId,
            UUID conversationId,
            UUID messageId,
            String traceId) {
        if (activeStreamCount(userId) >= chatProperties.maxConcurrentStreamsPerUser()) {
            throw new TooManyRequestsException("too many active chat streams");
        }
        StreamTask task = new StreamTask(userId, conversationId, messageId, traceId);
        StreamTask previous = tasks.putIfAbsent(task.key(), task);
        if (previous != null) {
            throw new TooManyRequestsException("chat stream is already active for this message");
        }
        return task;
    }

    public Optional<StreamTaskSnapshot> cancel(
            UUID userId,
            UUID conversationId,
            UUID messageId,
            StreamTermination termination) {
        StreamTask task = tasks.get(new StreamTaskKey(conversationId, messageId));
        if (task == null || !task.userId().equals(userId)) {
            return Optional.empty();
        }
        task.cancel(termination);
        return Optional.of(task.snapshot());
    }

    public void remove(StreamTask task) {
        tasks.remove(task.key(), task);
    }

    private long activeStreamCount(UUID userId) {
        return tasks.values().stream()
                .filter(task -> task.userId().equals(userId))
                .count();
    }

    private record StreamTaskKey(UUID conversationId, UUID messageId) {
    }

    public static final class StreamTask {

        private final StreamTaskKey key;
        private final UUID userId;
        private final String traceId;
        private final Instant startedAt;
        private final Sinks.One<StreamTermination> cancellation = Sinks.one();

        private StreamTask(UUID userId, UUID conversationId, UUID messageId, String traceId) {
            this.key = new StreamTaskKey(conversationId, messageId);
            this.userId = userId;
            this.traceId = traceId;
            this.startedAt = Instant.now();
        }

        public UUID userId() {
            return userId;
        }

        public UUID conversationId() {
            return key.conversationId();
        }

        public UUID messageId() {
            return key.messageId();
        }

        public String traceId() {
            return traceId;
        }

        public Instant startedAt() {
            return startedAt;
        }

        public Mono<StreamTermination> cancellationSignal() {
            return cancellation.asMono();
        }

        private StreamTaskKey key() {
            return key;
        }

        private void cancel(StreamTermination termination) {
            cancellation.tryEmitValue(termination);
        }

        private StreamTaskSnapshot snapshot() {
            return new StreamTaskSnapshot(
                    userId,
                    key.conversationId(),
                    key.messageId(),
                    traceId,
                    startedAt);
        }
    }

    public record StreamTaskSnapshot(
            UUID userId,
            UUID conversationId,
            UUID messageId,
            String traceId,
            Instant startedAt) {
    }
}
