package com.agentweave.tool.dto;

import com.agentweave.shared.security.CurrentUser;
import com.agentweave.tool.domain.ToolDefinitionEntity;
import com.agentweave.tool.domain.ToolRiskLevel;
import com.agentweave.tool.domain.ToolType;
import java.time.Instant;
import java.util.UUID;

public record ToolDefinitionResponse(
        UUID id,
        String code,
        String name,
        ToolType toolType,
        String description,
        String permissionCode,
        ToolRiskLevel riskLevel,
        boolean enabled,
        boolean available,
        String inputSchema,
        String outputSchema,
        Instant createdAt,
        Instant updatedAt) {

    public static ToolDefinitionResponse from(ToolDefinitionEntity definition, CurrentUser user) {
        boolean available = definition.isEnabled()
                && (user.hasRole("ADMIN") || user.hasPermission(definition.getPermissionCode()));
        return new ToolDefinitionResponse(
                definition.getId(),
                definition.getCode(),
                definition.getName(),
                definition.getToolType(),
                definition.getDescription(),
                definition.getPermissionCode(),
                definition.getRiskLevel(),
                definition.isEnabled(),
                available,
                definition.getInputSchema(),
                definition.getOutputSchema(),
                definition.getCreatedAt(),
                definition.getUpdatedAt());
    }
}
