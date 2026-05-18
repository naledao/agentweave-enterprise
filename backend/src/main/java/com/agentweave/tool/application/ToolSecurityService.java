package com.agentweave.tool.application;

import com.agentweave.auth.application.ToolPermissionService;
import com.agentweave.shared.audit.AuditLogService;
import com.agentweave.shared.exception.ResourceNotFoundException;
import com.agentweave.shared.exception.TooManyRequestsException;
import com.agentweave.shared.exception.ToolExecutionTimeoutException;
import com.agentweave.shared.exception.ToolPermissionDeniedException;
import com.agentweave.shared.security.CurrentUser;
import com.agentweave.shared.security.CurrentUserService;
import com.agentweave.tool.domain.ToolDefinitionEntity;
import com.agentweave.tool.domain.ToolInvocationEntity;
import com.agentweave.tool.domain.ToolRiskLevel;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

@Service
public class ToolSecurityService implements DisposableBean {

    private final CurrentUserService currentUserService;
    private final ToolDefinitionService toolDefinitionService;
    private final ToolPermissionService toolPermissionService;
    private final ToolInvocationRateLimiter rateLimiter;
    private final ToolSecurityProperties properties;
    private final AuditLogService auditLogService;
    private final ToolInvocationService toolInvocationService;
    private final Validator validator;
    private final ExecutorService executor;

    @Autowired
    public ToolSecurityService(
            CurrentUserService currentUserService,
            ToolDefinitionService toolDefinitionService,
            ToolPermissionService toolPermissionService,
            ToolInvocationRateLimiter rateLimiter,
            ToolSecurityProperties properties,
            AuditLogService auditLogService,
            ToolInvocationService toolInvocationService,
            Validator validator) {
        this.currentUserService = currentUserService;
        this.toolDefinitionService = toolDefinitionService;
        this.toolPermissionService = toolPermissionService;
        this.rateLimiter = rateLimiter;
        this.properties = properties;
        this.auditLogService = auditLogService;
        this.toolInvocationService = toolInvocationService;
        this.validator = validator;
        this.executor = Executors.newCachedThreadPool();
    }

    ToolSecurityService(
            CurrentUserService currentUserService,
            ToolDefinitionService toolDefinitionService,
            ToolPermissionService toolPermissionService,
            ToolInvocationRateLimiter rateLimiter,
            ToolSecurityProperties properties,
            AuditLogService auditLogService,
            ToolInvocationService toolInvocationService,
            Validator validator,
            ExecutorService executor) {
        this.currentUserService = currentUserService;
        this.toolDefinitionService = toolDefinitionService;
        this.toolPermissionService = toolPermissionService;
        this.rateLimiter = rateLimiter;
        this.properties = properties;
        this.auditLogService = auditLogService;
        this.toolInvocationService = toolInvocationService;
        this.validator = validator;
        this.executor = executor;
    }

    @Transactional(readOnly = true)
    public CurrentUser authorize(String permissionCode) {
        CurrentUser user = currentUserService.requireCurrentUser();
        ToolDefinitionEntity definition = requireDefinition(user, permissionCode);
        authorizeDefinition(user, definition);
        return user;
    }

    public Object invoke(
            String permissionCode,
            Object target,
            Method method,
            Object[] arguments,
            ToolInvocationCallback invocation) throws Throwable {
        CurrentUser user = currentUserService.requireCurrentUser();
        ToolDefinitionEntity definition = toolDefinitionService.findByPermissionCode(permissionCode)
                .orElse(null);
        String toolCode = definition == null ? permissionCode : definition.getCode();
        ToolInvocationEntity invocationRecord = toolInvocationService.start(definition, toolCode, user, method, arguments);
        if (definition == null) {
            ToolPermissionDeniedException ex = deny(user, permissionCode, "Tool is not whitelisted");
            toolInvocationService.markDenied(invocationRecord.getId(), ex.getMessage());
            throw ex;
        }
        try {
            authorizeDefinition(user, definition);
            validateArguments(user, definition.getPermissionCode(), target, method, arguments);
            requireRateLimit(user, definition.getPermissionCode());
            Object result = executeWithTimeout(definition.getPermissionCode(), invocation);
            toolInvocationService.markSuccess(invocationRecord.getId(), result);
            return result;
        } catch (ToolExecutionTimeoutException ex) {
            toolInvocationService.markTimeout(invocationRecord.getId(), ex);
            throw ex;
        } catch (ToolPermissionDeniedException | TooManyRequestsException | ConstraintViolationException ex) {
            toolInvocationService.markDenied(invocationRecord.getId(), ex.getMessage());
            throw ex;
        } catch (Throwable ex) {
            toolInvocationService.markFailure(invocationRecord.getId(), ex);
            throw ex;
        }
    }

    private ToolDefinitionEntity requireDefinition(CurrentUser user, String permissionCode) {
        return toolDefinitionService.findByPermissionCode(permissionCode)
                .orElseThrow(() -> deny(user, permissionCode, "Tool is not whitelisted"));
    }

    private void authorizeDefinition(CurrentUser user, ToolDefinitionEntity definition) {
        if (!definition.isEnabled()) {
            throw deny(user, definition.getPermissionCode(), "Tool is disabled");
        }
        if (definition.getRiskLevel() == ToolRiskLevel.HIGH && !user.hasRole("ADMIN")) {
            throw deny(user, definition.getPermissionCode(), "High risk tool requires administrator role");
        }
        if (!canInvokeTool(user, definition.getPermissionCode())) {
            throw deny(user, definition.getPermissionCode(), "Missing tool permission");
        }
    }

    private void requireRateLimit(CurrentUser user, String permissionCode) {
        try {
            rateLimiter.requireAllowed(user, permissionCode);
        } catch (TooManyRequestsException ex) {
            auditLogService.recordToolInvocationDenied(user, permissionCode, ex.getMessage());
            throw ex;
        }
    }

    private boolean canInvokeTool(CurrentUser user, String permissionCode) {
        try {
            return toolPermissionService.canInvokeTool(user, permissionCode);
        } catch (ResourceNotFoundException ex) {
            throw deny(user, permissionCode, "Tool permission is not defined");
        }
    }

    private void validateArguments(
            CurrentUser user,
            String permissionCode,
            Object target,
            Method method,
            Object[] arguments) {
        if (arguments == null) {
            return;
        }
        Set<ConstraintViolation<Object>> parameterViolations =
                validator.forExecutables().validateParameters(target, method, arguments);
        if (!parameterViolations.isEmpty()) {
            auditLogService.recordToolInvocationDenied(user, permissionCode, "Invalid tool arguments");
            throw new ConstraintViolationException(parameterViolations);
        }
        for (Object argument : arguments) {
            if (argument == null || isSimpleValue(argument)) {
                continue;
            }
            Set<ConstraintViolation<Object>> violations = validator.validate(argument);
            if (!violations.isEmpty()) {
                auditLogService.recordToolInvocationDenied(user, permissionCode, "Invalid tool arguments");
                throw new ConstraintViolationException(violations);
            }
        }
    }

    private Object executeWithTimeout(String permissionCode, ToolInvocationCallback invocation) throws Throwable {
        Duration timeout = properties.executionTimeout();
        SecurityContext securityContext = SecurityContextHolder.getContext();
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        Map<String, String> mdcContext = MDC.getCopyOfContextMap();
        Future<Object> future = executor.submit(() -> {
            try {
                SecurityContextHolder.setContext(securityContext);
                if (requestAttributes != null) {
                    RequestContextHolder.setRequestAttributes(requestAttributes, false);
                }
                if (mdcContext != null) {
                    MDC.setContextMap(mdcContext);
                } else {
                    MDC.clear();
                }
                return invocation.proceed();
            } catch (Throwable ex) {
                throw new ToolInvocationFailureException(ex);
            } finally {
                SecurityContextHolder.clearContext();
                RequestContextHolder.resetRequestAttributes();
                MDC.clear();
            }
        });
        try {
            return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException ex) {
            future.cancel(true);
            throw new ToolExecutionTimeoutException("tool execution timeout: " + permissionCode);
        } catch (ExecutionException ex) {
            if (ex.getCause() instanceof ToolInvocationFailureException failure) {
                throw failure.getCause();
            }
            throw ex.getCause();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw ex;
        }
    }

    private ToolPermissionDeniedException deny(CurrentUser user, String permissionCode, String reason) {
        auditLogService.recordToolInvocationDenied(user, permissionCode, reason);
        return new ToolPermissionDeniedException(reason);
    }

    private boolean isSimpleValue(Object argument) {
        return argument instanceof CharSequence
                || argument instanceof Number
                || argument instanceof Boolean
                || argument instanceof Character
                || argument instanceof Enum<?>
                || argument.getClass().isPrimitive();
    }

    @Override
    public void destroy() {
        executor.shutdownNow();
    }

    private static class ToolInvocationFailureException extends Exception {

        ToolInvocationFailureException(Throwable cause) {
            super(cause);
        }
    }
}
