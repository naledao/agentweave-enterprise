package com.agentweave.observability.dto;

import com.agentweave.conversation.domain.ModelCallLogEntity;
import java.time.Instant;
import java.util.UUID;

public record ModelCallResponse(
        UUID id,
        String traceId,
        UUID conversationId,
        UUID messageId,
        UUID workflowRunId,
        UUID workflowStepId,
        String provider,
        String modelName,
        String scenario,
        String promptSummary,
        String responseSummary,
        Integer inputTokens,
        Integer outputTokens,
        Integer totalTokens,
        long durationMs,
        String status,
        String errorCode,
        String errorMessage,
        Instant createdAt) {

    public static ModelCallResponse from(ModelCallLogEntity entity) {
        return new ModelCallResponse(
                entity.getId(),
                entity.getTraceId(),
                entity.getConversationId(),
                entity.getMessageId(),
                entity.getWorkflowRunId(),
                entity.getWorkflowStepId(),
                entity.getProvider(),
                entity.getModelName(),
                entity.getScenario().name(),
                entity.getPromptSummary(),
                entity.getResponseSummary(),
                entity.getInputTokens(),
                entity.getOutputTokens(),
                entity.getTotalTokens(),
                entity.getDurationMs(),
                entity.getStatus().name(),
                entity.getErrorCode(),
                entity.getErrorMessage(),
                entity.getCreatedAt());
    }
}
