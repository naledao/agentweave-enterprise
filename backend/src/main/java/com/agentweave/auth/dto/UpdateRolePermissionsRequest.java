package com.agentweave.auth.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.Set;
import java.util.UUID;

public record UpdateRolePermissionsRequest(@NotEmpty Set<UUID> permissionIds) {
}
