package com.agentweave.observability.dto;

import com.agentweave.shared.audit.AuditLogEntity;
import java.time.Instant;
import java.util.UUID;

public record AuditLogResponse(
        UUID id,
        String eventType,
        String resourceType,
        String resourceId,
        UUID userId,
        String username,
        String result,
        Long durationMs,
        String requestSummary,
        String responseSummary,
        String errorMessage,
        String traceId,
        Instant createdAt) {

    public static AuditLogResponse from(AuditLogEntity entity) {
        return new AuditLogResponse(
                entity.getId(),
                entity.getEventType().name(),
                entity.getResourceType(),
                entity.getResourceId(),
                entity.getUserId(),
                entity.getUsername(),
                entity.getResult().name(),
                entity.getDurationMs(),
                entity.getRequestSummary(),
                entity.getResponseSummary(),
                entity.getErrorMessage(),
                entity.getTraceId(),
                entity.getCreatedAt());
    }
}
