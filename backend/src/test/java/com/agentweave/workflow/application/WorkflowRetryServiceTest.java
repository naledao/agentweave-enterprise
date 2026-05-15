package com.agentweave.workflow.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agentweave.workflow.domain.AgentStepType;
import com.agentweave.workflow.dto.AgentExecutionResult;
import com.agentweave.workflow.dto.WorkflowPlan;
import com.agentweave.workflow.dto.WorkflowPlanStep;
import com.agentweave.workflow.dto.WorkflowRetryRequest;
import com.agentweave.workflow.dto.WorkflowReviewResult;
import com.agentweave.workflow.state.AgentWorkflowState;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("WorkflowRetryService")
class WorkflowRetryServiceTest {

    @Mock
    private WorkflowRecoveryService recoveryService;

    @Mock
    private WorkflowRunService workflowRunService;

    @Mock
    private AgentStepService agentStepService;

    @Test
    @DisplayName("should prepare retry state and record retry count")
    void shouldPrepareRetryStateAndRecordRetryCount() {
        WorkflowRetryService service = new WorkflowRetryService(recoveryService, workflowRunService, agentStepService);
        UUID runId = UUID.randomUUID();
        WorkflowPlan plan = WorkflowPlan.of("goal", List.of(
                WorkflowPlanStep.of(0, AgentStepType.RAG_SEARCH, "search"),
                WorkflowPlanStep.of(1, AgentStepType.TOOL_CALL, "call"),
                WorkflowPlanStep.of(2, AgentStepType.REVIEW, "review")));
        AgentExecutionResult step0 = AgentExecutionResult.success("s0", List.of(), List.of(), List.of());
        AgentExecutionResult step1 = AgentExecutionResult.success("s1", List.of(), List.of(), List.of());
        AgentExecutionResult step2 = AgentExecutionResult.success("s2", List.of(), List.of(), List.of());
        AgentWorkflowState state = new AgentWorkflowState(Map.of(
                AgentWorkflowState.RUN_ID, runId,
                AgentWorkflowState.PLAN, plan,
                AgentWorkflowState.CURRENT_STEP_INDEX, 2,
                AgentWorkflowState.STEP_RESULTS, List.of(step0, step1, step2),
                AgentWorkflowState.CITATIONS, List.of(new WorkflowReviewResult.Citation("doc", "chunk", "snippet", 0.9, "source")),
                AgentWorkflowState.GRAPH_PATHS, List.of(new WorkflowReviewResult.GraphPath(List.of("a", "b"), List.of("REL"), "a -> b")),
                AgentWorkflowState.TOOL_CALLS, List.of(new WorkflowReviewResult.ToolCallResult("tool:ticket:query", Map.of(), "ok", true, null))));

        when(recoveryService.prepareResume(runId)).thenReturn(state);

        AgentWorkflowState restored = service.prepareRetry(runId, new WorkflowRetryRequest(1, "tool timeout"));

        assertThat(restored.currentStepIndex()).isEqualTo(1);
        assertThat(restored.stepResults()).hasSize(1);
        verify(workflowRunService).prepareForRecovery(eq(runId), any(), eq(1), eq(true));
        verify(agentStepService).recordRetry(runId, 2, "tool timeout");
    }

    @Test
    @DisplayName("should record planner retry on step zero when plan has not been created")
    void shouldRecordPlannerRetryOnStepZeroWhenPlanHasNotBeenCreated() {
        WorkflowRetryService service = new WorkflowRetryService(recoveryService, workflowRunService, agentStepService);
        UUID runId = UUID.randomUUID();
        AgentWorkflowState state = new AgentWorkflowState(Map.of(
                AgentWorkflowState.RUN_ID, runId,
                AgentWorkflowState.CURRENT_STEP_INDEX, 0,
                AgentWorkflowState.STEP_RESULTS, List.of()));

        when(recoveryService.prepareResume(runId)).thenReturn(state);

        service.prepareRetry(runId, new WorkflowRetryRequest(0, "planner timeout"));

        verify(agentStepService).recordRetry(runId, 0, "planner timeout");
    }
}
