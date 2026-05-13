package com.agentweave.knowledge.messaging.event;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record DocumentProcessingEvent(
        UUID eventId,
        String eventType,
        int payloadVersion,
        UUID documentId,
        String traceId,
        UUID triggeredBy,
        Instant occurredAt,
        Map<String, String> metadata) {

    public static DocumentProcessingEvent create(
            DocumentProcessingEventType eventType,
            UUID documentId,
            String traceId,
            UUID triggeredBy,
            Map<String, String> metadata) {
        return new DocumentProcessingEvent(
                UUID.randomUUID(),
                eventType.routingKey(),
                1,
                documentId,
                traceId,
                triggeredBy,
                Instant.now(),
                metadata == null ? Map.of() : Map.copyOf(metadata));
    }
}
