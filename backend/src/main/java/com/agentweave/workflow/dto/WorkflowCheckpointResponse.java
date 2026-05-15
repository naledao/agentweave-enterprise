package com.agentweave.workflow.dto;

import com.agentweave.workflow.domain.WorkflowCheckpointEntity;
import java.time.Instant;
import java.util.UUID;

public record WorkflowCheckpointResponse(
        UUID checkpointId,
        UUID runId,
        int stepIndex,
        String nodeName,
        int stateVersion,
        String checksum,
        boolean recoverable,
        String errorCode,
        String errorMessage,
        Instant createdAt
) {

    public static WorkflowCheckpointResponse from(WorkflowCheckpointEntity entity, boolean recoverable, String errorCode, String errorMessage) {
        return new WorkflowCheckpointResponse(
                entity.getId(),
                entity.getRun().getId(),
                entity.getStepIndex(),
                entity.getNodeName(),
                entity.getStateVersion(),
                entity.getChecksum(),
                recoverable,
                errorCode,
                errorMessage,
                entity.getCreatedAt());
    }
}
