package com.agentweave.workflow.dto;

import com.agentweave.workflow.domain.AgentStepEntity;
import com.agentweave.workflow.domain.AgentStepStatus;
import com.agentweave.workflow.domain.AgentStepType;
import java.time.Instant;
import java.util.UUID;

public record WorkflowStepResponse(
        UUID stepId,
        int stepIndex,
        AgentStepType stepType,
        String nodeName,
        AgentStepStatus status,
        String inputSummary,
        String outputSummary,
        Instant startedAt,
        Instant finishedAt,
        Long durationMs,
        String errorCode,
        String errorMessage
) {

    public static WorkflowStepResponse from(AgentStepEntity entity) {
        return new WorkflowStepResponse(
                entity.getId(),
                entity.getStepIndex(),
                entity.getStepType(),
                entity.getNodeName(),
                entity.getStatus(),
                entity.getInputSummary(),
                entity.getOutputSummary(),
                entity.getStartedAt(),
                entity.getFinishedAt(),
                entity.getDurationMs(),
                entity.getErrorCode(),
                entity.getErrorMessage());
    }
}
