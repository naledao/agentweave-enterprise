package com.agentweave.workflow.dto;

import com.agentweave.workflow.domain.AgentStepType;
import java.util.List;
import java.util.UUID;

public record WorkflowPlanStep(
        UUID stepId,
        Integer stepIndex,
        AgentStepType stepType,
        String instruction,
        List<String> requiredInputs,
        List<String> expectedOutputs,
        String riskLevel,
        String toolCode,
        String retrievalMode
) {
    public WorkflowPlanStep {
        if (requiredInputs == null) {
            requiredInputs = List.of();
        }
        if (expectedOutputs == null) {
            expectedOutputs = List.of();
        }
    }

    public static WorkflowPlanStep of(Integer stepIndex, AgentStepType stepType, String instruction) {
        return new WorkflowPlanStep(
                UUID.randomUUID(),
                stepIndex,
                stepType,
                instruction,
                List.of(),
                List.of(),
                null,
                null,
                null
        );
    }

    public boolean isToolCall() {
        return stepType == AgentStepType.TOOL_CALL;
    }

    public boolean isRagSearch() {
        return stepType == AgentStepType.RAG_SEARCH || stepType == AgentStepType.GRAPH_RAG_SEARCH;
    }

    public boolean isHighRisk() {
        return "HIGH".equalsIgnoreCase(riskLevel);
    }
}