package com.agentweave.workflow.node;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.agentweave.conversation.application.ConversationRagService;
import com.agentweave.langchain4j.agent.AgentPromptTemplateService;
import com.agentweave.langchain4j.agent.ExecutorAgent;
import com.agentweave.langchain4j.agent.PlannerAgent;
import com.agentweave.langchain4j.agent.ReviewerAgent;
import com.agentweave.tool.application.ToolDefinitionService;
import com.agentweave.tool.application.ToolRiskEvaluator;
import com.agentweave.workflow.application.WorkflowApprovalService;
import com.agentweave.workflow.application.AgentStepService;
import com.agentweave.workflow.application.PlanValidator;
import com.agentweave.workflow.application.WorkflowCheckpointService;
import com.agentweave.workflow.application.WorkflowRunService;
import com.agentweave.workflow.domain.AgentStepType;
import com.agentweave.workflow.dto.WorkflowPlan;
import com.agentweave.workflow.dto.WorkflowPlanStep;
import com.agentweave.workflow.graph.WorkflowNodeNames;
import com.agentweave.workflow.state.AgentWorkflowState;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("WorkflowNodeExecutor routing")
class WorkflowNodeExecutorRouteTest {

    private final WorkflowCheckpointService checkpointService = mock(WorkflowCheckpointService.class);
    private final ToolRiskEvaluator toolRiskEvaluator = new ToolRiskEvaluator(mock(ToolDefinitionService.class));

    private final WorkflowNodeExecutor executor = new WorkflowNodeExecutor(
            mock(PlannerAgent.class),
            mock(ExecutorAgent.class),
            mock(ReviewerAgent.class),
            mock(PlanValidator.class),
            mock(WorkflowRunService.class),
            mock(AgentStepService.class),
            mock(AgentPromptTemplateService.class),
            mock(ConversationRagService.class),
            checkpointService,
            mock(ToolDefinitionService.class),
            toolRiskEvaluator,
            mock(WorkflowApprovalService.class));

    @Test
    @DisplayName("should route high-risk tool step to human approval")
    void shouldRouteHighRiskToolStepToHumanApproval() {
        WorkflowPlan plan = WorkflowPlan.of("goal", List.of(
                new WorkflowPlanStep(UUID.randomUUID(), 0, AgentStepType.TOOL_CALL,
                        "send notification", List.of(), List.of(), "HIGH", "tool:ticket:query", null)));
        AgentWorkflowState state = new AgentWorkflowState(Map.of(
                AgentWorkflowState.PLAN, plan,
                AgentWorkflowState.CURRENT_STEP_INDEX, 0));

        Map<String, Object> updates = executor.routeStep(state);

        assertThat(updates).containsEntry(AgentWorkflowState.NEXT_NODE, WorkflowNodeNames.HUMAN_APPROVAL_NODE);
    }

    @Test
    @DisplayName("should route approved high-risk tool step to tool node")
    void shouldRouteApprovedHighRiskToolStepToToolNode() {
        WorkflowPlan plan = WorkflowPlan.of("goal", List.of(
                new WorkflowPlanStep(UUID.randomUUID(), 0, AgentStepType.TOOL_CALL,
                        "send notification", List.of(), List.of(), "HIGH", "tool:ticket:query", null)));
        AgentWorkflowState state = new AgentWorkflowState(Map.of(
                AgentWorkflowState.PLAN, plan,
                AgentWorkflowState.CURRENT_STEP_INDEX, 0,
                AgentWorkflowState.APPROVAL_STATUS, "APPROVED"));

        Map<String, Object> updates = executor.routeStep(state);

        assertThat(updates).containsEntry(AgentWorkflowState.NEXT_NODE, WorkflowNodeNames.TOOL_NODE);
    }
}
