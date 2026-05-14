package com.agentweave.tool.dto;

import com.agentweave.tool.domain.ToolInvocationEntity;
import java.time.Instant;
import java.util.UUID;

public record ToolInvocationResponse(
        UUID id,
        String toolCode,
        UUID userId,
        String username,
        UUID conversationId,
        UUID messageId,
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
                invocation.getUserId(),
                invocation.getUsername(),
                invocation.getConversationId(),
                invocation.getMessageId(),
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
