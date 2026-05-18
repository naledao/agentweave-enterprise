package com.agentweave.shared.audit;

import com.agentweave.shared.exception.AccessDeniedBusinessException;
import com.agentweave.shared.exception.TooManyRequestsException;
import com.agentweave.shared.exception.ToolPermissionDeniedException;
import com.agentweave.shared.security.CurrentUser;
import com.agentweave.shared.security.CurrentUserService;
import jakarta.validation.ConstraintViolationException;
import java.lang.reflect.Method;
import java.util.Optional;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.annotation.Order;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

@Aspect
@Component
@Order(200)
public class AuditLogAspect {

    private final AuditLogService auditLogService;
    private final CurrentUserService currentUserService;
    private final AuditSummarySanitizer auditSummarySanitizer;
    private final ExpressionParser expressionParser = new SpelExpressionParser();
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    public AuditLogAspect(
            AuditLogService auditLogService,
            CurrentUserService currentUserService,
            AuditSummarySanitizer auditSummarySanitizer) {
        this.auditLogService = auditLogService;
        this.currentUserService = currentUserService;
        this.auditSummarySanitizer = auditSummarySanitizer;
    }

    @Around("@annotation(auditLog)")
    public Object record(ProceedingJoinPoint joinPoint, AuditLog auditLog) throws Throwable {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        try {
            Object result = joinPoint.proceed();
            stopWatch.stop();
            save(joinPoint, method, auditLog, AuditResult.SUCCESS, stopWatch.getTotalTimeMillis(), result, null);
            return result;
        } catch (Throwable ex) {
            if (stopWatch.isRunning()) {
                stopWatch.stop();
            }
            save(joinPoint, method, auditLog, failureResult(ex), stopWatch.getTotalTimeMillis(), null, ex);
            throw ex;
        }
    }

    private void save(
            ProceedingJoinPoint joinPoint,
            Method method,
            AuditLog auditLog,
            AuditResult result,
            long durationMs,
            Object response,
            Throwable failure) {
        Optional<CurrentUser> user = currentUserService.getCurrentUser();
        auditLogService.record(new AuditLogCommand(
                auditLog.eventType(),
                user.map(CurrentUser::id).orElse(null),
                user.map(CurrentUser::username).orElse(null),
                auditLog.resourceType(),
                resourceId(joinPoint, method, auditLog.resourceId(), response),
                method.getName(),
                result,
                durationMs,
                auditLog.includeRequest() ? auditSummarySanitizer.summarize(joinPoint.getArgs()) : null,
                auditLog.includeResponse() ? auditSummarySanitizer.summarize(response) : null,
                errorSummary(failure)));
    }

    private String resourceId(ProceedingJoinPoint joinPoint, Method method, String expression, Object response) {
        if (expression == null || expression.isBlank()) {
            return null;
        }
        try {
            if (!expression.trim().startsWith("#")) {
                return auditSummarySanitizer.sanitizeText(expression, 120);
            }
            StandardEvaluationContext context = new StandardEvaluationContext();
            context.setVariable("result", response);
            String[] parameterNames = parameterNameDiscoverer.getParameterNames(method);
            Object[] args = joinPoint.getArgs();
            for (int i = 0; i < args.length; i++) {
                context.setVariable("p" + i, args[i]);
                context.setVariable("a" + i, args[i]);
                context.setVariable("arg" + i, args[i]);
            }
            if (parameterNames != null) {
                for (int i = 0; i < parameterNames.length; i++) {
                    context.setVariable(parameterNames[i], args[i]);
                }
            }
            Expression parsed = expressionParser.parseExpression(expression);
            Object value = parsed.getValue(context);
            return value == null ? null : auditSummarySanitizer.sanitizeText(String.valueOf(value), 120);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private AuditResult failureResult(Throwable failure) {
        if (failure instanceof AccessDeniedBusinessException
                || failure instanceof ToolPermissionDeniedException
                || failure instanceof TooManyRequestsException
                || failure instanceof ConstraintViolationException) {
            return AuditResult.DENIED;
        }
        return AuditResult.FAILURE;
    }

    private String errorSummary(Throwable failure) {
        if (failure == null) {
            return null;
        }
        String message = failure.getMessage();
        if (message == null || message.isBlank()) {
            message = failure.getClass().getSimpleName();
        }
        return auditSummarySanitizer.sanitizeText(message, 500);
    }
}
