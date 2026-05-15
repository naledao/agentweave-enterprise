package com.agentweave.workflow.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agentweave.workflow.domain.AgentRunEntity;
import com.agentweave.workflow.domain.WorkflowRunStatus;
import com.agentweave.workflow.dto.WorkflowRunResponse;
import com.agentweave.workflow.graph.AgentWorkflowGraph;
import com.agentweave.workflow.state.AgentWorkflowState;
import java.util.Map;
import java.util.UUID;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphInput;
import org.bsc.langgraph4j.RunnableConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("WorkflowExecutionService")
class WorkflowExecutionServiceTest {

    @Mock
    private AgentWorkflowGraph agentWorkflowGraph;

    @Mock
    private CompiledGraph<AgentWorkflowState> compiledGraph;

    @Mock
    private WorkflowRunService workflowRunService;

    @Mock
    private WorkflowRecoveryService workflowRecoveryService;

    @Mock
    private WorkflowRetryService workflowRetryService;

    private WorkflowExecutionService workflowExecutionService;
    private UUID runId;
    private AgentRunEntity run;

    @BeforeEach
    void setUp() {
        workflowExecutionService = new WorkflowExecutionService(
                agentWorkflowGraph,
                workflowRunService,
                workflowRecoveryService,
                workflowRetryService);
        runId = UUID.randomUUID();
        run = new AgentRunEntity(runId, UUID.randomUUID(), "Test goal");
        run.setConversationId(UUID.randomUUID());
        run.setTraceId("trace-123");
        when(agentWorkflowGraph.compiledGraph()).thenReturn(compiledGraph);
    }

    @Test
    @DisplayName("should invoke compiled graph with initial workflow state")
    void shouldInvokeCompiledGraphWithInitialState() throws Exception {
        WorkflowRunResponse expected = response(WorkflowRunStatus.SUCCEEDED);
        when(workflowRunService.getEntityById(runId)).thenReturn(run);
        when(workflowRunService.getInternal(runId)).thenReturn(expected);

        WorkflowRunResponse actual = workflowExecutionService.executeWorkflow(runId);

        assertThat(actual).isEqualTo(expected);
        verify(compiledGraph).invoke(any(GraphInput.class), any(RunnableConfig.class));
    }

    @Test
    @DisplayName("should resume compiled graph from latest checkpoint")
    void shouldResumeCompiledGraphFromLatestCheckpoint() throws Exception {
        WorkflowRunResponse expected = response(WorkflowRunStatus.SUCCEEDED);
        AgentWorkflowState checkpointState = new AgentWorkflowState(Map.of(
                AgentWorkflowState.RUN_ID, runId,
                AgentWorkflowState.GOAL, "Test goal"));
        when(workflowRecoveryService.prepareResume(runId)).thenReturn(checkpointState);
        when(workflowRecoveryService.recoveryStartNode(checkpointState)).thenReturn("load_context");
        when(workflowRunService.getInternal(runId)).thenReturn(expected);
        when(compiledGraph.updateState(any(RunnableConfig.class), any(), any()))
                .thenReturn(RunnableConfig.builder().threadId(runId.toString()).nextNode("planner").build());

        WorkflowRunResponse actual = workflowExecutionService.resumeWorkflow(runId);

        assertThat(actual).isEqualTo(expected);
        verify(compiledGraph).updateState(any(RunnableConfig.class), any(), any());
        verify(compiledGraph).invoke(any(GraphInput.class), any(RunnableConfig.class));
    }

    private WorkflowRunResponse response(WorkflowRunStatus status) {
        return new WorkflowRunResponse(
                runId,
                run.getConversationId(),
                run.getUserId(),
                run.getGoal(),
                status,
                0,
                "answer",
                run.getTraceId(),
                null,
                null,
                null);
    }
}
