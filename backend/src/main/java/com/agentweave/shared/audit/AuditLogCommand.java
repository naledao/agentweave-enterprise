package com.agentweave.shared.audit;

import java.util.UUID;

public record AuditLogCommand(
        AuditEventType eventType,
        UUID userId,
        String username,
        String resourceType,
        String resourceId,
        String action,
        AuditResult result,
        Long durationMs,
        String requestSummary,
        String responseSummary,
        String errorMessage) {
}
