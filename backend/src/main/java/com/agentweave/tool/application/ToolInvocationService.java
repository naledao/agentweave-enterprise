package com.agentweave.tool.application;

import com.agentweave.observability.application.AgentWeaveMetrics;
import com.agentweave.shared.exception.ResourceNotFoundException;
import com.agentweave.shared.audit.AuditEventType;
import com.agentweave.shared.audit.AuditLogCommand;
import com.agentweave.shared.audit.AuditLogService;
import com.agentweave.shared.audit.AuditResult;
import com.agentweave.shared.security.CurrentUser;
import com.agentweave.shared.security.CurrentUserService;
import com.agentweave.shared.tracing.CorrelationContext;
import com.agentweave.shared.tracing.TraceIdProvider;
import com.agentweave.tool.domain.ToolDefinitionEntity;
import com.agentweave.tool.domain.ToolInvocationEntity;
import com.agentweave.tool.domain.ToolInvocationStatus;
import com.agentweave.tool.domain.ToolRiskLevel;
import com.agentweave.tool.domain.ToolType;
import com.agentweave.tool.dto.ToolInvocationListResponse;
import com.agentweave.tool.dto.ToolInvocationQueryRequest;
import com.agentweave.tool.dto.ToolInvocationResponse;
import com.agentweave.tool.repository.ToolInvocationRepository;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ToolInvocationService {

    private static final int SUMMARY_MAX_LENGTH = 1000;
    private static final int ERROR_MAX_LENGTH = 500;

    private final ToolInvocationRepository toolInvocationRepository;
    private final CurrentUserService currentUserService;
    private final TraceIdProvider traceIdProvider;
    private final AuditLogService auditLogService;
    private final AgentWeaveMetrics agentWeaveMetrics;

    public ToolInvocationService(
            ToolInvocationRepository toolInvocationRepository,
            CurrentUserService currentUserService,
            TraceIdProvider traceIdProvider,
            AuditLogService auditLogService,
            AgentWeaveMetrics agentWeaveMetrics) {
        this.toolInvocationRepository = toolInvocationRepository;
        this.currentUserService = currentUserService;
        this.traceIdProvider = traceIdProvider;
        this.auditLogService = auditLogService;
        this.agentWeaveMetrics = agentWeaveMetrics;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ToolInvocationEntity start(
            ToolDefinitionEntity definition,
            String fallbackToolCode,
            CurrentUser user,
            Method method,
            Object[] arguments) {
        String toolCode = definition == null ? fallbackToolCode : definition.getCode();
        ToolInvocationEntity invocation = new ToolInvocationEntity(
                UUID.randomUUID(),
                normalizeToolCode(toolCode),
                normalizeToolName(definition, toolCode),
                toolType(definition),
                riskLevel(definition),
                user.id(),
                user.username(),
                currentCorrelationId(CorrelationContext.CONVERSATION_ID_KEY),
                currentCorrelationId(CorrelationContext.MESSAGE_ID_KEY),
                currentCorrelationId(CorrelationContext.WORKFLOW_RUN_ID_KEY),
                currentCorrelationId(CorrelationContext.WORKFLOW_STEP_ID_KEY),
                inputSummary(method, arguments),
                ToolInvocationStatus.RUNNING,
                traceIdProvider.currentTraceId());
        return toolInvocationRepository.saveAndFlush(invocation);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSuccess(UUID invocationId, Object result) {
        ToolInvocationEntity invocation = require(invocationId);
        invocation.succeed(resultSummary(result), Instant.now());
        ToolInvocationEntity saved = toolInvocationRepository.saveAndFlush(invocation);
        agentWeaveMetrics.recordToolCall(saved);
        recordAudit(saved, AuditEventType.TOOL_INVOKE, AuditResult.SUCCESS);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailure(UUID invocationId, Throwable failure) {
        ToolInvocationEntity invocation = require(invocationId);
        invocation.fail(errorSummary(failure), Instant.now());
        ToolInvocationEntity saved = toolInvocationRepository.saveAndFlush(invocation);
        agentWeaveMetrics.recordToolCall(saved);
        recordAudit(saved, AuditEventType.TOOL_INVOKE, AuditResult.FAILURE);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markTimeout(UUID invocationId, Throwable failure) {
        ToolInvocationEntity invocation = require(invocationId);
        invocation.timeout(errorSummary(failure), Instant.now());
        ToolInvocationEntity saved = toolInvocationRepository.saveAndFlush(invocation);
        agentWeaveMetrics.recordToolCall(saved);
        recordAudit(saved, AuditEventType.TOOL_INVOKE, AuditResult.FAILURE);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markDenied(UUID invocationId, String reason) {
        ToolInvocationEntity invocation = require(invocationId);
        invocation.deny(sanitize(reason, ERROR_MAX_LENGTH), Instant.now());
        ToolInvocationEntity saved = toolInvocationRepository.saveAndFlush(invocation);
        agentWeaveMetrics.recordToolCall(saved);
        recordAudit(saved, AuditEventType.TOOL_DENIED, AuditResult.DENIED);
    }

    @Transactional(readOnly = true)
    public ToolInvocationListResponse list(ToolInvocationQueryRequest request) {
        CurrentUser user = currentUserService.requireCurrentUser();
        Pageable pageable = PageRequest.of(
                request.pageNumber(),
                request.pageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ToolInvocationResponse> invocations = toolInvocationRepository
                .findAll(querySpec(request, user), pageable)
                .map(ToolInvocationResponse::from);
        return ToolInvocationListResponse.from(invocations);
    }

    @Transactional(readOnly = true)
    public ToolInvocationResponse get(UUID invocationId) {
        CurrentUser user = currentUserService.requireCurrentUser();
        ToolInvocationEntity invocation = toolInvocationRepository.findById(invocationId)
                .filter(item -> canRead(user, item))
                .orElseThrow(() -> new ResourceNotFoundException("tool invocation not found"));
        return ToolInvocationResponse.from(invocation);
    }

    private ToolInvocationEntity require(UUID invocationId) {
        return toolInvocationRepository.findById(invocationId)
                .orElseThrow(() -> new ResourceNotFoundException("tool invocation not found"));
    }

    private Specification<ToolInvocationEntity> querySpec(ToolInvocationQueryRequest request, CurrentUser user) {
        Specification<ToolInvocationEntity> spec = readableBy(user);
        spec = and(spec, toolCodeEquals(request.normalizedToolCode()));
        spec = and(spec, toolTypeEquals(request.normalizedToolType()));
        spec = and(spec, statusEquals(request.normalizedStatus()));
        spec = and(spec, createdAtGreaterThanOrEqualTo(request.createdFrom()));
        return and(spec, createdAtLessThanOrEqualTo(request.createdTo()));
    }

    private Specification<ToolInvocationEntity> and(
            Specification<ToolInvocationEntity> left,
            Specification<ToolInvocationEntity> right) {
        if (right == null) {
            return left;
        }
        return left.and(right);
    }

    private Specification<ToolInvocationEntity> readableBy(CurrentUser user) {
        if (user.hasRole("ADMIN") || user.hasPermission("observability:read")) {
            return unrestricted();
        }
        return (root, query, builder) -> builder.equal(root.get("userId"), user.id());
    }

    private Specification<ToolInvocationEntity> unrestricted() {
        return (root, query, builder) -> builder.conjunction();
    }

    private Specification<ToolInvocationEntity> toolCodeEquals(String toolCode) {
        if (toolCode == null) {
            return null;
        }
        return (root, query, builder) -> builder.equal(root.get("toolCode"), toolCode);
    }

    private Specification<ToolInvocationEntity> toolTypeEquals(ToolType toolType) {
        if (toolType == null) {
            return null;
        }
        return (root, query, builder) -> builder.equal(root.get("toolType"), toolType);
    }

    private Specification<ToolInvocationEntity> statusEquals(ToolInvocationStatus status) {
        if (status == null) {
            return null;
        }
        return (root, query, builder) -> builder.equal(root.get("status"), status);
    }

    private Specification<ToolInvocationEntity> createdAtGreaterThanOrEqualTo(Instant createdFrom) {
        if (createdFrom == null) {
            return null;
        }
        return (root, query, builder) -> builder.greaterThanOrEqualTo(root.get("createdAt"), createdFrom);
    }

    private Specification<ToolInvocationEntity> createdAtLessThanOrEqualTo(Instant createdTo) {
        if (createdTo == null) {
            return null;
        }
        return (root, query, builder) -> builder.lessThanOrEqualTo(root.get("createdAt"), createdTo);
    }

    private boolean canRead(CurrentUser user, ToolInvocationEntity invocation) {
        return user.hasRole("ADMIN")
                || user.hasPermission("observability:read")
                || user.id().equals(invocation.getUserId());
    }

    private String inputSummary(Method method, Object[] arguments) {
        if (arguments == null || arguments.length == 0) {
            return method.getName() + "()";
        }
        return sanitize(method.getName() + "(" + Arrays.toString(arguments) + ")", SUMMARY_MAX_LENGTH);
    }

    private String resultSummary(Object result) {
        if (result == null) {
            return null;
        }
        return sanitize(String.valueOf(result), SUMMARY_MAX_LENGTH);
    }

    private String errorSummary(Throwable failure) {
        String message = failure == null ? null : failure.getMessage();
        if (message == null || message.isBlank()) {
            message = failure == null ? "Unknown tool invocation failure" : failure.getClass().getSimpleName();
        }
        return sanitize(message, ERROR_MAX_LENGTH);
    }

    private String sanitize(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String sanitized = value
                .replaceAll("(?i)(api[-_ ]?key|token|secret|password)=\\S+", "$1=******")
                .replaceAll("(?i)(api[-_ ]?key|token|secret|password)\\s*:\\s*\\S+", "$1:******")
                .trim();
        if (sanitized.length() <= maxLength) {
            return sanitized;
        }
        return sanitized.substring(0, maxLength);
    }

    private String normalizeToolCode(String toolCode) {
        if (toolCode == null || toolCode.isBlank()) {
            return "unknown";
        }
        return toolCode.trim();
    }

    private String normalizeToolName(ToolDefinitionEntity definition, String toolCode) {
        if (definition != null && definition.getName() != null && !definition.getName().isBlank()) {
            return sanitize(definition.getName(), 160);
        }
        return normalizeToolCode(toolCode);
    }

    private ToolRiskLevel riskLevel(ToolDefinitionEntity definition) {
        return definition == null ? null : definition.getRiskLevel();
    }

    private ToolType toolType(ToolDefinitionEntity definition) {
        return definition == null ? ToolType.UNKNOWN : definition.getToolType();
    }

    private void recordAudit(
            ToolInvocationEntity invocation,
            AuditEventType eventType,
            AuditResult result) {
        auditLogService.record(new AuditLogCommand(
                eventType,
                invocation.getUserId(),
                invocation.getUsername(),
                "tool",
                invocation.getToolCode(),
                "INVOKE_TOOL",
                result,
                invocation.getDurationMs(),
                invocation.getInputSummary(),
                invocation.getResultSummary(),
                invocation.getErrorMessage()));
    }

    private UUID currentCorrelationId(String key) {
        String value = MDC.get(key);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
