package com.agentweave.auth.dto;

import com.agentweave.auth.domain.RoleEntity;
import com.agentweave.auth.domain.UserEntity;
import com.agentweave.auth.domain.UserStatus;
import com.agentweave.shared.security.CurrentUser;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String username,
        String displayName,
        String email,
        UserStatus status,
        Set<String> roles,
        Set<String> permissions,
        Instant lastLoginAt) {

    public static UserResponse from(UserEntity user) {
        Set<String> roleCodes = user.getRoles().stream()
                .map(RoleEntity::getCode)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        Set<String> permissions = user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(permission -> permission.getCode())
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getEmail(),
                user.getStatus(),
                roleCodes,
                permissions,
                user.getLastLoginAt());
    }

    public static UserResponse from(CurrentUser user) {
        return new UserResponse(
                user.id(),
                user.username(),
                user.displayName(),
                null,
                UserStatus.ACTIVE,
                user.roles(),
                user.permissions(),
                null);
    }

    public List<String> sortedRoles() {
        return roles.stream().sorted().toList();
    }
}
