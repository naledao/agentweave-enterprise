package com.agentweave.tool.dto;

import com.agentweave.tool.domain.ToolInvocationEntity;
import java.time.Instant;
import java.util.UUID;

public record ToolInvocationResponse(
        UUID id,
        String toolCode,
        String toolName,
        String toolType,
        String riskLevel,
        UUID userId,
        String username,
        UUID conversationId,
        UUID messageId,
        UUID workflowRunId,
        UUID workflowStepId,
        String inputSummary,
        String resultSummary,
        String status,
        Long durationMs,
        String errorMessage,
        String traceId,
        Instant createdAt,
        Instant finishedAt) {

    public static ToolInvocationResponse from(ToolInvocationEntity invocation) {
        return new ToolInvocationResponse(
                invocation.getId(),
                invocation.getToolCode(),
                invocation.getToolName(),
                invocation.getToolType() == null ? null : invocation.getToolType().name(),
                invocation.getRiskLevel() == null ? null : invocation.getRiskLevel().name(),
                invocation.getUserId(),
                invocation.getUsername(),
                invocation.getConversationId(),
                invocation.getMessageId(),
                invocation.getWorkflowRunId(),
                invocation.getWorkflowStepId(),
                invocation.getInputSummary(),
                invocation.getResultSummary(),
                invocation.getStatus().value(),
                invocation.getDurationMs(),
                invocation.getErrorMessage(),
                invocation.getTraceId(),
                invocation.getCreatedAt(),
                invocation.getFinishedAt());
    }
}
