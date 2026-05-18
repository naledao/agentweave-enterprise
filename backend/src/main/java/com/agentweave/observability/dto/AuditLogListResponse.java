package com.agentweave.observability.dto;

import java.util.List;
import org.springframework.data.domain.Page;

public record AuditLogListResponse(
        List<AuditLogResponse> items,
        int page,
        int size,
        long total,
        int totalPages) {

    public static AuditLogListResponse from(Page<AuditLogResponse> auditLogs) {
        return new AuditLogListResponse(
                auditLogs.getContent(),
                auditLogs.getNumber(),
                auditLogs.getSize(),
                auditLogs.getTotalElements(),
                auditLogs.getTotalPages());
    }
}
