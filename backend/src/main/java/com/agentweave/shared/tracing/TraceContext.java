package com.agentweave.shared.tracing;

import java.util.Optional;
import java.util.UUID;
import org.slf4j.MDC;

public record TraceContext(String traceId, UUID conversationId, UUID messageId) {

    public TraceContext {
        if (traceId == null || traceId.isBlank()) {
            throw new IllegalArgumentException("traceId must not be blank");
        }
    }

    public static TraceContext of(String traceId, UUID conversationId, UUID messageId) {
        return new TraceContext(traceId, conversationId, messageId);
    }

    public static Optional<TraceContext> fromMdc() {
        String traceId = MDC.get(TraceIdProvider.TRACE_ID_KEY);
        if (traceId == null || traceId.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new TraceContext(
                traceId,
                parseUuid(MDC.get(CorrelationContext.CONVERSATION_ID_KEY)),
                parseUuid(MDC.get(CorrelationContext.MESSAGE_ID_KEY))));
    }

    private static UUID parseUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
