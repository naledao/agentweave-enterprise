package com.agentweave.shared.security;

import com.agentweave.shared.exception.ApiErrorResponse;
import com.agentweave.shared.exception.ErrorCode;
import com.agentweave.shared.audit.AuditLogService;
import com.agentweave.shared.tracing.TraceIdProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
public class JsonAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;
    private final TraceIdProvider traceIdProvider;
    private final AuditLogService auditLogService;

    public JsonAccessDeniedHandler(
            ObjectMapper objectMapper,
            TraceIdProvider traceIdProvider,
            AuditLogService auditLogService) {
        this.objectMapper = objectMapper;
        this.traceIdProvider = traceIdProvider;
        this.auditLogService = auditLogService;
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException {
        String traceId = traceIdProvider.currentTraceId(request);
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("X-Trace-Id", traceId);
        auditLogService.recordPermissionDenied(
                "HTTP",
                request.getRequestURI(),
                request.getMethod(),
                accessDeniedException.getMessage());
        ApiErrorResponse body = new ApiErrorResponse(
                ErrorCode.ACCESS_DENIED.code(),
                ErrorCode.ACCESS_DENIED.defaultMessage(),
                request.getRequestURI(),
                traceId,
                Instant.now());
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
