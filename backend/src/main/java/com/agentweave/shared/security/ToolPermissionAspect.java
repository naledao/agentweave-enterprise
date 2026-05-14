package com.agentweave.shared.security;

import com.agentweave.tool.application.ToolSecurityService;
import java.lang.reflect.Method;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Aspect
@Order(200)
@Component
public class ToolPermissionAspect {

    private final ToolSecurityService toolSecurityService;

    public ToolPermissionAspect(ToolSecurityService toolSecurityService) {
        this.toolSecurityService = toolSecurityService;
    }

    @Around("@annotation(requireToolPermission)")
    public Object requireToolPermission(
            ProceedingJoinPoint joinPoint,
            RequireToolPermission requireToolPermission) throws Throwable {
        Method method = resolveMethod(joinPoint);
        return toolSecurityService.invoke(
                requireToolPermission.value(),
                joinPoint.getTarget(),
                method,
                joinPoint.getArgs(),
                joinPoint::proceed);
    }

    private Method resolveMethod(ProceedingJoinPoint joinPoint) throws NoSuchMethodException {
        Method signatureMethod = ((MethodSignature) joinPoint.getSignature()).getMethod();
        return joinPoint.getTarget().getClass()
                .getMethod(signatureMethod.getName(), signatureMethod.getParameterTypes());
    }
}
