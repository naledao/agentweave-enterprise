package com.agentweave.shared.audit;

import com.agentweave.shared.security.CurrentUser;
import com.agentweave.shared.security.CurrentUserService;
import com.agentweave.shared.tracing.TraceIdProvider;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLogService {

    private static final String X_FORWARDED_FOR = "X-Forwarded-For";
    private static final String USER_AGENT = "User-Agent";

    private final AuditLogRepository auditLogRepository;
    private final ObjectProvider<HttpServletRequest> requestProvider;
    private final TraceIdProvider traceIdProvider;
    private final CurrentUserService currentUserService;

    public AuditLogService(
            AuditLogRepository auditLogRepository,
            ObjectProvider<HttpServletRequest> requestProvider,
            TraceIdProvider traceIdProvider,
            CurrentUserService currentUserService) {
        this.auditLogRepository = auditLogRepository;
        this.requestProvider = requestProvider;
        this.traceIdProvider = traceIdProvider;
        this.currentUserService = currentUserService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordLoginSuccess(UUID userId, String username) {
        save(AuditEventType.LOGIN_SUCCESS, userId, username, "USER",
                userId.toString(), "LOGIN", AuditResult.SUCCESS, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordLoginFailure(String username, String reason) {
        save(AuditEventType.LOGIN_FAILED, null, username, "USER", username,
                "LOGIN", AuditResult.FAILED, reason);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordLogout(CurrentUser user) {
        save(AuditEventType.LOGOUT, user.id(), user.username(), "USER",
                user.id().toString(), "LOGOUT", AuditResult.SUCCESS, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordPermissionDenied(String targetType, String targetId, String action, String reason) {
        Optional<CurrentUser> currentUser = currentUserService.getCurrentUser();
        save(AuditEventType.PERMISSION_DENIED,
                currentUser.map(CurrentUser::id).orElse(null),
                currentUser.map(CurrentUser::username).orElse(null),
                targetType,
                targetId,
                action,
                AuditResult.DENIED,
                reason);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordToolPermissionDenied(CurrentUser user, String permissionCode) {
        recordToolInvocationDenied(user, permissionCode, "Missing tool permission");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordToolInvocationDenied(CurrentUser user, String permissionCode, String reason) {
        save(AuditEventType.TOOL_PERMISSION_DENIED, user.id(), user.username(), "TOOL",
                permissionCode, "INVOKE_TOOL", AuditResult.DENIED, truncateReason(reason));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordUserProfileUpdated(UUID targetUserId) {
        Optional<CurrentUser> actor = currentUserService.getCurrentUser();
        save(AuditEventType.USER_PROFILE_UPDATED,
                actor.map(CurrentUser::id).orElse(null),
                actor.map(CurrentUser::username).orElse(null),
                "USER",
                targetUserId.toString(),
                "UPDATE_USER_PROFILE",
                AuditResult.SUCCESS,
                null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordUserPasswordReset(UUID targetUserId) {
        Optional<CurrentUser> actor = currentUserService.getCurrentUser();
        save(AuditEventType.USER_PASSWORD_RESET,
                actor.map(CurrentUser::id).orElse(null),
                actor.map(CurrentUser::username).orElse(null),
                "USER",
                targetUserId.toString(),
                "RESET_USER_PASSWORD",
                AuditResult.SUCCESS,
                null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordUserRoleChanged(UUID targetUserId, Set<String> nextRoleCodes) {
        Optional<CurrentUser> actor = currentUserService.getCurrentUser();
        String roleCodes = nextRoleCodes.stream()
                .sorted()
                .collect(Collectors.joining(","));
        save(AuditEventType.USER_ROLE_CHANGED,
                actor.map(CurrentUser::id).orElse(null),
                actor.map(CurrentUser::username).orElse(null),
                "USER",
                targetUserId.toString(),
                "UPDATE_USER_ROLES",
                AuditResult.SUCCESS,
                "roles=" + roleCodes);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordUserStatusChanged(UUID targetUserId, String previousStatus, String nextStatus) {
        Optional<CurrentUser> actor = currentUserService.getCurrentUser();
        save(AuditEventType.USER_STATUS_CHANGED,
                actor.map(CurrentUser::id).orElse(null),
                actor.map(CurrentUser::username).orElse(null),
                "USER",
                targetUserId.toString(),
                "UPDATE_USER_STATUS",
                AuditResult.SUCCESS,
                "status=" + previousStatus + "->" + nextStatus);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordRoleCreated(UUID roleId, String roleCode) {
        Optional<CurrentUser> actor = currentUserService.getCurrentUser();
        save(AuditEventType.ROLE_CREATED,
                actor.map(CurrentUser::id).orElse(null),
                actor.map(CurrentUser::username).orElse(null),
                "ROLE",
                roleId.toString(),
                "CREATE_ROLE",
                AuditResult.SUCCESS,
                "role=" + roleCode);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordRoleUpdated(UUID roleId, String roleCode) {
        Optional<CurrentUser> actor = currentUserService.getCurrentUser();
        save(AuditEventType.ROLE_UPDATED,
                actor.map(CurrentUser::id).orElse(null),
                actor.map(CurrentUser::username).orElse(null),
                "ROLE",
                roleId.toString(),
                "UPDATE_ROLE",
                AuditResult.SUCCESS,
                "role=" + roleCode);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordRolePermissionsChanged(UUID roleId, String roleCode, Set<String> permissionCodes) {
        Optional<CurrentUser> actor = currentUserService.getCurrentUser();
        String permissions = permissionCodes.stream()
                .sorted()
                .collect(Collectors.joining(","));
        save(AuditEventType.ROLE_PERMISSION_CHANGED,
                actor.map(CurrentUser::id).orElse(null),
                actor.map(CurrentUser::username).orElse(null),
                "ROLE",
                roleId.toString(),
                "UPDATE_ROLE_PERMISSIONS",
                AuditResult.SUCCESS,
                "role=" + roleCode + ";permissions=" + permissions);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordRoleStatusChanged(UUID roleId, String roleCode, String previousStatus, String nextStatus) {
        Optional<CurrentUser> actor = currentUserService.getCurrentUser();
        save(AuditEventType.ROLE_STATUS_CHANGED,
                actor.map(CurrentUser::id).orElse(null),
                actor.map(CurrentUser::username).orElse(null),
                "ROLE",
                roleId.toString(),
                "UPDATE_ROLE_STATUS",
                AuditResult.SUCCESS,
                "role=" + roleCode + ";status=" + previousStatus + "->" + nextStatus);
    }

    private void save(
            AuditEventType eventType,
            UUID userId,
            String username,
            String targetType,
            String targetId,
            String action,
            AuditResult result,
            String reason) {
        HttpServletRequest request = requestProvider.getIfAvailable();
        AuditLogEntity entity = new AuditLogEntity(
                UUID.randomUUID(),
                eventType,
                userId,
                username,
                targetType,
                targetId,
                action,
                result,
                reason,
                ipAddress(request),
                userAgent(request),
                traceId(request));
        auditLogRepository.save(entity);
    }

    private String traceId(HttpServletRequest request) {
        if (request == null) {
            return traceIdProvider.currentTraceId();
        }
        return traceIdProvider.currentTraceId(request);
    }

    private String truncateReason(String reason) {
        if (reason == null || reason.length() <= 500) {
            return reason;
        }
        return reason.substring(0, 500);
    }

    private String ipAddress(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String forwardedFor = request.getHeader(X_FORWARDED_FOR);
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String userAgent(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        return request.getHeader(USER_AGENT);
    }
}
