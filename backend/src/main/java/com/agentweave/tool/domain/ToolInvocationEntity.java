package com.agentweave.tool.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "tool_invocations")
public class ToolInvocationEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 120)
    private String toolCode;

    @Column(nullable = false, length = 160)
    private String toolName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ToolType toolType;

    @Enumerated(EnumType.STRING)
    @Column(length = 40)
    private ToolRiskLevel riskLevel;

    private UUID userId;

    @Column(length = 80)
    private String username;

    private UUID conversationId;

    private UUID messageId;

    private UUID workflowRunId;

    private UUID workflowStepId;

    @Column(columnDefinition = "TEXT")
    private String inputSummary;

    @Column(columnDefinition = "TEXT")
    private String resultSummary;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ToolInvocationStatus status;

    private Long durationMs;

    @Column(length = 500)
    private String errorMessage;

    @Column(length = 120)
    private String traceId;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant finishedAt;

    protected ToolInvocationEntity() {
    }

    public ToolInvocationEntity(
            UUID id,
            String toolCode,
            String toolName,
            ToolRiskLevel riskLevel,
            UUID userId,
            String username,
            UUID conversationId,
            UUID messageId,
            UUID workflowRunId,
            UUID workflowStepId,
            String inputSummary,
            ToolInvocationStatus status,
            String traceId) {
        this(
                id,
                toolCode,
                toolName,
                ToolType.UNKNOWN,
                riskLevel,
                userId,
                username,
                conversationId,
                messageId,
                workflowRunId,
                workflowStepId,
                inputSummary,
                status,
                traceId);
    }

    public ToolInvocationEntity(
            UUID id,
            String toolCode,
            String toolName,
            ToolType toolType,
            ToolRiskLevel riskLevel,
            UUID userId,
            String username,
            UUID conversationId,
            UUID messageId,
            UUID workflowRunId,
            UUID workflowStepId,
            String inputSummary,
            ToolInvocationStatus status,
            String traceId) {
        this.id = id;
        this.toolCode = toolCode;
        this.toolName = toolName;
        this.toolType = toolType == null ? ToolType.UNKNOWN : toolType;
        this.riskLevel = riskLevel;
        this.userId = userId;
        this.username = username;
        this.conversationId = conversationId;
        this.messageId = messageId;
        this.workflowRunId = workflowRunId;
        this.workflowStepId = workflowStepId;
        this.inputSummary = inputSummary;
        this.status = status;
        this.traceId = traceId;
    }

    public void succeed(String resultSummary, Instant finishedAt) {
        finish(ToolInvocationStatus.SUCCESS, resultSummary, null, finishedAt);
    }

    public void fail(String errorMessage, Instant finishedAt) {
        finish(ToolInvocationStatus.FAILED, null, errorMessage, finishedAt);
    }

    public void timeout(String errorMessage, Instant finishedAt) {
        finish(ToolInvocationStatus.TIMEOUT, null, errorMessage, finishedAt);
    }

    public void deny(String errorMessage, Instant finishedAt) {
        finish(ToolInvocationStatus.DENIED, null, errorMessage, finishedAt);
    }

    private void finish(
            ToolInvocationStatus status,
            String resultSummary,
            String errorMessage,
            Instant finishedAt) {
        this.status = status;
        this.resultSummary = resultSummary;
        this.errorMessage = errorMessage;
        this.finishedAt = finishedAt;
        if (createdAt != null && finishedAt != null) {
            this.durationMs = Math.max(0, Duration.between(createdAt, finishedAt).toMillis());
        } else {
            this.durationMs = 0L;
        }
    }

    public UUID getId() {
        return id;
    }

    public String getToolCode() {
        return toolCode;
    }

    public String getToolName() {
        return toolName;
    }

    public ToolType getToolType() {
        return toolType;
    }

    public ToolRiskLevel getRiskLevel() {
        return riskLevel;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public UUID getConversationId() {
        return conversationId;
    }

    public UUID getMessageId() {
        return messageId;
    }

    public UUID getWorkflowRunId() {
        return workflowRunId;
    }

    public UUID getWorkflowStepId() {
        return workflowStepId;
    }

    public String getInputSummary() {
        return inputSummary;
    }

    public String getResultSummary() {
        return resultSummary;
    }

    public ToolInvocationStatus getStatus() {
        return status;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getTraceId() {
        return traceId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }
}
