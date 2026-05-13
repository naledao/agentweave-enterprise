package com.agentweave.auth.dto;

import com.agentweave.auth.domain.RoleStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateRoleStatusRequest(@NotNull RoleStatus status) {
}
