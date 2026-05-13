package com.agentweave.conversation.dto;

public record WorkflowStepEventResponse(
        String workflowRunId,
        String stepName,
        String status,
        String traceId) {
}
