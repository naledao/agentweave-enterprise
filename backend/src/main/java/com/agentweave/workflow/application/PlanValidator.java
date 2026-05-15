package com.agentweave.workflow.application;

import com.agentweave.workflow.domain.AgentStepType;
import com.agentweave.workflow.dto.WorkflowPlan;
import com.agentweave.workflow.dto.WorkflowPlanStep;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
public class PlanValidator {

    private static final Set<AgentStepType> ALLOWED_STEP_TYPES = Set.of(
            AgentStepType.RAG_SEARCH,
            AgentStepType.GRAPH_RAG_SEARCH,
            AgentStepType.TOOL_CALL,
            AgentStepType.HUMAN_APPROVAL,
            AgentStepType.FINAL_ANSWER,
            AgentStepType.REVIEW
    );

    public ValidationResult validate(WorkflowPlan plan) {
        if (plan == null) {
            return ValidationResult.invalid("Plan cannot be null");
        }

        if (plan.steps() == null || plan.steps().isEmpty()) {
            return ValidationResult.invalid("Plan must contain at least one step");
        }

        List<WorkflowPlanStep> steps = plan.steps();

        // Check step index continuity
        for (int i = 0; i < steps.size(); i++) {
            WorkflowPlanStep step = steps.get(i);
            if (step.stepIndex() == null || step.stepIndex() != i) {
                return ValidationResult.invalid("Step index must be continuous, expected " + i + " but got " + step.stepIndex());
            }
        }

        // Validate each step
        for (WorkflowPlanStep step : steps) {
            StepValidationResult stepResult = validateStep(step);
            if (!stepResult.passed()) {
                return ValidationResult.invalid("Step " + step.stepIndex() + ": " + stepResult.message());
            }
        }

        // Check final step type
        WorkflowPlanStep lastStep = steps.get(steps.size() - 1);
        if (lastStep.stepType() != AgentStepType.FINAL_ANSWER && lastStep.stepType() != AgentStepType.REVIEW) {
            return ValidationResult.invalid("Plan must end with FINAL_ANSWER or REVIEW step");
        }

        return ValidationResult.valid();
    }

    private StepValidationResult validateStep(WorkflowPlanStep step) {
        if (step.stepType() == null) {
            return StepValidationResult.invalid("Step type is required");
        }

        if (!ALLOWED_STEP_TYPES.contains(step.stepType())) {
            return StepValidationResult.invalid("Step type " + step.stepType() + " is not allowed");
        }

        if (step.instruction() == null || step.instruction().isBlank()) {
            return StepValidationResult.invalid("Instruction is required");
        }

        // Tool steps must have tool code
        if (step.stepType() == AgentStepType.TOOL_CALL) {
            if (step.toolCode() == null || step.toolCode().isBlank()) {
                return StepValidationResult.invalid("Tool steps must specify tool code");
            }
        }

        // High risk tool steps must have risk level
        if (step.stepType() == AgentStepType.TOOL_CALL && step.isHighRisk()) {
            if (step.riskLevel() == null || step.riskLevel().isBlank()) {
                return StepValidationResult.invalid("High risk tool steps must specify risk level");
            }
        }

        // RAG steps must have retrieval mode
        if (step.stepType() == AgentStepType.RAG_SEARCH || step.stepType() == AgentStepType.GRAPH_RAG_SEARCH) {
            if (step.retrievalMode() == null || step.retrievalMode().isBlank()) {
                return StepValidationResult.invalid("RAG steps must specify retrieval mode");
            }
        }

        return StepValidationResult.valid();
    }

    public record ValidationResult(boolean passed, String message) {
        public static ValidationResult valid() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult invalid(String message) {
            return new ValidationResult(false, message);
        }
    }

    private record StepValidationResult(boolean passed, String message) {
        public static StepValidationResult valid() {
            return new StepValidationResult(true, null);
        }

        public static StepValidationResult invalid(String message) {
            return new StepValidationResult(false, message);
        }
    }
}