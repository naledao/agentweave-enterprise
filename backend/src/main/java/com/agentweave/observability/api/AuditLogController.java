package com.agentweave.observability.api;

import com.agentweave.observability.application.AuditLogQueryService;
import com.agentweave.observability.dto.AuditLogListResponse;
import com.agentweave.observability.dto.AuditLogQueryRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/observability/audit-logs")
public class AuditLogController {

    private final AuditLogQueryService auditLogQueryService;

    public AuditLogController(AuditLogQueryService auditLogQueryService) {
        this.auditLogQueryService = auditLogQueryService;
    }

    @GetMapping
    public AuditLogListResponse list(@Valid AuditLogQueryRequest request) {
        return auditLogQueryService.list(request);
    }
}
