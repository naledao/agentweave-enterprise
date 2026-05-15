package com.agentweave.workflow.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agentweave.shared.exception.BusinessException;
import com.agentweave.workflow.domain.AgentRunEntity;
import com.agentweave.workflow.domain.WorkflowCheckpointEntity;
import com.agentweave.workflow.domain.WorkflowRunStatus;
import com.agentweave.workflow.dto.WorkflowCheckpointResponse;
import com.agentweave.workflow.dto.WorkflowPlan;
import com.agentweave.workflow.dto.WorkflowPlanStep;
import com.agentweave.workflow.state.AgentWorkflowState;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("WorkflowRecoveryService")
class WorkflowRecoveryServiceTest {

    @Mock
    private WorkflowRunService workflowRunService;

    @Mock
    private WorkflowCheckpointService checkpointService;

    private WorkflowRecoveryService workflowRecoveryService;
    private UUID runId;
    private AgentRunEntity run;

    @BeforeEach
    void setUp() {
        workflowRecoveryService = new WorkflowRecoveryService(workflowRunService, checkpointService);
        runId = UUID.randomUUID();
        run = new AgentRunEntity(runId, UUID.randomUUID(), "Test goal");
    }

    @Test
    @DisplayName("should return latest checkpoint response")
    void shouldReturnLatestCheckpointResponse() {
        WorkflowCheckpointEntity checkpoint = new WorkflowCheckpointEntity(
                UUID.randomUUID(),
                run,
                2,
                "tool_node",
                WorkflowCheckpointService.STATE_VERSION,
                "{\"runId\":\"" + runId + "\"}",
                "checksum");
        AgentWorkflowState state = new AgentWorkflowState(Map.of(
                AgentWorkflowState.RUN_ID, runId,
                AgentWorkflowState.CURRENT_STEP_INDEX, 2));

        when(workflowRunService.getAccessibleEntityById(runId)).thenReturn(run);
        when(checkpointService.latestCheckpoint(runId)).thenReturn(Optional.of(checkpoint));
        when(checkpointService.toState(checkpoint)).thenReturn(state);

        WorkflowCheckpointResponse response = workflowRecoveryService.latestCheckpoint(runId);

        assertThat(response.runId()).isEqualTo(runId);
        assertThat(response.nodeName()).isEqualTo("tool_node");
        assertThat(response.stepIndex()).isEqualTo(2);
    }

    @Test
    @DisplayName("should prepare recoverable workflow run")
    void shouldPrepareRecoverableWorkflowRun() {
        WorkflowPlan plan = WorkflowPlan.of("goal", List.of(
                WorkflowPlanStep.of(0, com.agentweave.workflow.domain.AgentStepType.RAG_SEARCH, "search"),
                WorkflowPlanStep.of(1, com.agentweave.workflow.domain.AgentStepType.REVIEW, "review")));
        AgentWorkflowState state = new AgentWorkflowState(Map.of(
                AgentWorkflowState.RUN_ID, runId,
                AgentWorkflowState.PLAN, plan,
                AgentWorkflowState.CURRENT_STEP_INDEX, 1,
                AgentWorkflowState.ERROR, new AgentWorkflowState.WorkflowError(
                        "TOOL_TIMEOUT", "timeout", "tool_node", 1, true)));

        when(workflowRunService.getAccessibleEntityById(runId)).thenReturn(run);
        when(checkpointService.latestState(runId)).thenReturn(Optional.of(state));

        AgentWorkflowState restored = workflowRecoveryService.prepareResume(runId);

        assertThat(restored.hasError()).isFalse();
        verify(workflowRunService).prepareForRecovery(eq(runId), eq(WorkflowRunStatus.EXECUTING), eq(1), eq(false));
    }

    @Test
    @DisplayName("should reject terminal run recovery")
    void shouldRejectTerminalRunRecovery() {
        run.succeed("done", java.time.Instant.now());
        when(workflowRunService.getAccessibleEntityById(runId)).thenReturn(run);

        assertThatThrownBy(() -> workflowRecoveryService.prepareResume(runId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("cannot be recovered");
    }
}
