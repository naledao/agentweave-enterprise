package com.agentweave.auth.dto;

import com.agentweave.auth.domain.RoleEntity;
import com.agentweave.auth.domain.RoleStatus;
import java.util.List;
import java.util.UUID;

public record RoleResponse(
        UUID id,
        String code,
        String name,
        String description,
        RoleStatus status,
        List<PermissionResponse> permissions) {

    public static RoleResponse from(RoleEntity role) {
        return new RoleResponse(
                role.getId(),
                role.getCode(),
                role.getName(),
                role.getDescription(),
                role.getStatus(),
                role.getPermissions().stream()
                        .map(PermissionResponse::from)
                        .toList());
    }
}
