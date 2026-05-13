package com.agentweave.shared.security;

import com.agentweave.auth.application.ToolPermissionService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Aspect
@Order(200)
@Component
public class ToolPermissionAspect {

    private final ToolPermissionService toolPermissionService;

    public ToolPermissionAspect(ToolPermissionService toolPermissionService) {
        this.toolPermissionService = toolPermissionService;
    }

    @Around("@annotation(requireToolPermission)")
    public Object requireToolPermission(
            ProceedingJoinPoint joinPoint,
            RequireToolPermission requireToolPermission) throws Throwable {
        toolPermissionService.requireToolPermission(requireToolPermission.value());
        return joinPoint.proceed();
    }
}
