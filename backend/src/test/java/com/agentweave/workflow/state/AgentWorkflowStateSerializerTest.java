package com.agentweave.workflow.state;

import static org.assertj.core.api.Assertions.assertThat;

import com.agentweave.workflow.domain.AgentStepType;
import com.agentweave.workflow.dto.WorkflowPlan;
import com.agentweave.workflow.dto.WorkflowPlanStep;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AgentWorkflowStateSerializer")
class AgentWorkflowStateSerializerTest {

    private final AgentWorkflowStateSerializer serializer = new AgentWorkflowStateSerializer(new ObjectMapper());

    @Test
    @DisplayName("should round-trip structured workflow state")
    void shouldRoundTripStructuredWorkflowState() {
        UUID runId = UUID.randomUUID();
        WorkflowPlan plan = WorkflowPlan.of("goal", List.of(
                new WorkflowPlanStep(UUID.randomUUID(), 0, AgentStepType.TOOL_CALL,
                        "query ticket", List.of(), List.of(), "HIGH", "tool:ticket:query", null)));
        AgentWorkflowState state = new AgentWorkflowState(Map.of(
                AgentWorkflowState.RUN_ID, runId,
                AgentWorkflowState.GOAL, "goal",
                AgentWorkflowState.PLAN, plan,
                AgentWorkflowState.CURRENT_STEP_INDEX, 0));

        AgentWorkflowState restored = serializer.readPayload(serializer.writePayload(state));

        assertThat(restored.runId()).isEqualTo(runId);
        assertThat(restored.plan().steps()).hasSize(1);
        assertThat(restored.plan().steps().get(0).stepType()).isEqualTo(AgentStepType.TOOL_CALL);
        assertThat(restored.plan().steps().get(0).isHighRisk()).isTrue();
    }
}
