package com.agentweave.workflow.dto;

import com.agentweave.workflow.domain.AgentRunEntity;
import com.agentweave.workflow.domain.WorkflowRunStatus;
import java.time.Instant;
import java.util.UUID;

public record WorkflowRunListItemResponse(
        UUID runId,
        UUID conversationId,
        UUID userId,
        String goal,
        WorkflowRunStatus status,
        int currentStepIndex,
        String finalAnswer,
        String errorCode,
        String errorMessage,
        String traceId,
        Instant startedAt,
        Instant finishedAt,
        Instant createdAt,
        Instant updatedAt
) {

    public static WorkflowRunListItemResponse from(AgentRunEntity entity) {
        return new WorkflowRunListItemResponse(
                entity.getId(),
                entity.getConversationId(),
                entity.getUserId(),
                entity.getGoal(),
                entity.getStatus(),
                entity.getCurrentStepIndex(),
                entity.getFinalAnswer(),
                entity.getErrorCode(),
                entity.getErrorMessage(),
                entity.getTraceId(),
                entity.getStartedAt(),
                entity.getFinishedAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
