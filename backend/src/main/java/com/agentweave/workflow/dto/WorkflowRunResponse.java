package com.agentweave.workflow.dto;

import com.agentweave.workflow.domain.AgentRunEntity;
import com.agentweave.workflow.domain.WorkflowRunStatus;
import java.time.Instant;
import java.util.UUID;

public record WorkflowRunResponse(
        UUID runId,
        UUID conversationId,
        UUID userId,
        String goal,
        WorkflowRunStatus status,
        int currentStepIndex,
        String finalAnswer,
        String traceId,
        Instant startedAt,
        Instant finishedAt,
        Instant createdAt
) {

    public static WorkflowRunResponse from(AgentRunEntity entity) {
        return new WorkflowRunResponse(
                entity.getId(),
                entity.getConversationId(),
                entity.getUserId(),
                entity.getGoal(),
                entity.getStatus(),
                entity.getCurrentStepIndex(),
                entity.getFinalAnswer(),
                entity.getTraceId(),
                entity.getStartedAt(),
                entity.getFinishedAt(),
                entity.getCreatedAt());
    }
}
