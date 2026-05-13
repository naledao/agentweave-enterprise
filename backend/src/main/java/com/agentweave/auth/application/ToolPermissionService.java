package com.agentweave.auth.application;

import com.agentweave.auth.domain.PermissionType;
import com.agentweave.auth.repository.PermissionRepository;
import com.agentweave.shared.audit.AuditLogService;
import com.agentweave.shared.exception.AccessDeniedBusinessException;
import com.agentweave.shared.exception.ResourceNotFoundException;
import com.agentweave.shared.security.CurrentUser;
import com.agentweave.shared.security.CurrentUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ToolPermissionService {

    private static final Logger log = LoggerFactory.getLogger(ToolPermissionService.class);

    private final CurrentUserService currentUserService;
    private final PermissionRepository permissionRepository;
    private final AuditLogService auditLogService;

    public ToolPermissionService(
            CurrentUserService currentUserService,
            PermissionRepository permissionRepository,
            AuditLogService auditLogService) {
        this.currentUserService = currentUserService;
        this.permissionRepository = permissionRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public boolean canInvokeTool(CurrentUser user, String permissionCode) {
        if (!permissionRepository.findByCode(permissionCode)
                .filter(permission -> permission.getType() == PermissionType.TOOL)
                .isPresent()) {
            throw new ResourceNotFoundException("tool permission not found");
        }
        return user.hasRole("ADMIN") || user.hasPermission(permissionCode);
    }

    @Transactional(readOnly = true)
    public void requireToolPermission(String permissionCode) {
        CurrentUser user = currentUserService.requireCurrentUser();
        if (!canInvokeTool(user, permissionCode)) {
            log.warn("Tool permission denied: user={}, permission={}", user.username(), permissionCode);
            auditLogService.recordToolPermissionDenied(user, permissionCode);
            throw new AccessDeniedBusinessException();
        }
    }
}
