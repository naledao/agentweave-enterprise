package com.agentweave.workflow.dto;

import com.agentweave.workflow.domain.AgentStepType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record WorkflowPlan(
        UUID planId,
        String goal,
        List<WorkflowPlanStep> steps,
        Instant createdAt,
        String createdBy,
        Integer estimatedSteps,
        String riskSummary
) {
    public WorkflowPlan {
        if (steps == null) {
            steps = List.of();
        }
    }

    public static WorkflowPlan of(String goal, List<WorkflowPlanStep> steps) {
        return new WorkflowPlan(
                UUID.randomUUID(),
                goal,
                steps,
                Instant.now(),
                "planner",
                steps.size(),
                null
        );
    }
}