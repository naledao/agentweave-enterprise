package com.agentweave.auth.dto;

import com.agentweave.auth.domain.PermissionEntity;
import com.agentweave.auth.domain.PermissionType;
import java.util.UUID;

public record PermissionResponse(
        UUID id,
        String code,
        String name,
        PermissionType type,
        String description) {

    public static PermissionResponse from(PermissionEntity permission) {
        return new PermissionResponse(
                permission.getId(),
                permission.getCode(),
                permission.getName(),
                permission.getType(),
                permission.getDescription());
    }
}
