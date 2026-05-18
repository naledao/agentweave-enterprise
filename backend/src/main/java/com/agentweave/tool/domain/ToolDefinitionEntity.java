package com.agentweave.tool.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "tool_definitions")
public class ToolDefinitionEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 120)
    private String code;

    @Column(nullable = false, length = 160)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ToolType toolType;

    @Column(length = 500)
    private String description;

    @Column(nullable = false, length = 120)
    private String permissionCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ToolRiskLevel riskLevel;

    @Column(nullable = false)
    private boolean enabled;

    @Column(columnDefinition = "TEXT")
    private String inputSchema;

    @Column(columnDefinition = "TEXT")
    private String outputSchema;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    protected ToolDefinitionEntity() {
    }

    public ToolDefinitionEntity(
            UUID id,
            String code,
            String name,
            String description,
            String permissionCode,
            ToolRiskLevel riskLevel,
            boolean enabled,
            String inputSchema,
            String outputSchema) {
        this(
                id,
                code,
                name,
                defaultToolType(code),
                description,
                permissionCode,
                riskLevel,
                enabled,
                inputSchema,
                outputSchema);
    }

    public ToolDefinitionEntity(
            UUID id,
            String code,
            String name,
            ToolType toolType,
            String description,
            String permissionCode,
            ToolRiskLevel riskLevel,
            boolean enabled,
            String inputSchema,
            String outputSchema) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.toolType = toolType == null ? ToolType.UNKNOWN : toolType;
        this.description = description;
        this.permissionCode = permissionCode;
        this.riskLevel = riskLevel;
        this.enabled = enabled;
        this.inputSchema = inputSchema;
        this.outputSchema = outputSchema;
    }

    public UUID getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public ToolType getToolType() {
        return toolType;
    }

    public String getDescription() {
        return description;
    }

    public String getPermissionCode() {
        return permissionCode;
    }

    public ToolRiskLevel getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(ToolRiskLevel riskLevel) {
        this.riskLevel = riskLevel;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getInputSchema() {
        return inputSchema;
    }

    public String getOutputSchema() {
        return outputSchema;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    private static ToolType defaultToolType(String code) {
        if (code == null || code.isBlank()) {
            return ToolType.UNKNOWN;
        }
        String normalized = code.trim().toLowerCase(Locale.ROOT);
        if (normalized.contains("log")) {
            return ToolType.LOG_SEARCH;
        }
        if (normalized.contains("endpoint") || normalized.contains("status")) {
            return ToolType.ENDPOINT_STATUS;
        }
        return ToolType.BUSINESS_QUERY;
    }
}
