package com.agentweave.workflow.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agentweave.langchain4j.tool.EndpointTools;
import com.agentweave.langchain4j.tool.LogTools;
import com.agentweave.langchain4j.tool.TicketTools;
import com.agentweave.shared.tracing.CorrelationContext;
import com.agentweave.tool.ticket.TicketQueryRequest;
import com.agentweave.tool.ticket.TicketQueryResult;
import com.agentweave.workflow.domain.AgentRunEntity;
import com.agentweave.workflow.domain.AgentStepEntity;
import com.agentweave.workflow.domain.AgentStepType;
import com.agentweave.workflow.dto.AgentExecutionResult;
import com.agentweave.workflow.dto.WorkflowPlanStep;
import com.agentweave.workflow.state.AgentWorkflowState;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.MDC;

class WorkflowToolExecutionServiceTest {

    private final TicketTools ticketTools = mock(TicketTools.class);
    private final WorkflowToolExecutionService service = new WorkflowToolExecutionService(
            ticketTools,
            mock(LogTools.class),
            mock(EndpointTools.class),
            new CorrelationContext());

    @Test
    void ticketToolExecutesWithWorkflowCorrelation() {
        UUID runId = UUID.randomUUID();
        UUID stepId = UUID.randomUUID();
        AgentRunEntity run = new AgentRunEntity(runId, UUID.randomUUID(), "query ticket INC-10002");
        AgentStepEntity stepEntity = new AgentStepEntity(stepId, run, 1, AgentStepType.TOOL_CALL, "tool_node");
        WorkflowPlanStep planStep = new WorkflowPlanStep(
                UUID.randomUUID(),
                0,
                AgentStepType.TOOL_CALL,
                "query ticket INC-10002",
                List.of(),
                List.of(),
                "LOW",
                "tool:ticket:query",
                null);
        when(ticketTools.queryTicket(any(TicketQueryRequest.class)))
                .thenAnswer(invocation -> {
                    assertThat(MDC.get(CorrelationContext.WORKFLOW_RUN_ID_KEY)).isEqualTo(runId.toString());
                    assertThat(MDC.get(CorrelationContext.WORKFLOW_STEP_ID_KEY)).isEqualTo(stepId.toString());
                    return new TicketQueryResult(
                            "INC-10002",
                            "Payment API timeout",
                            "OPEN",
                            "P2",
                            "ops",
                            Instant.parse("2026-05-17T00:00:00Z"));
                });

        AgentExecutionResult result = service.execute(state(runId), planStep, stepEntity);

        assertThat(result.success()).isTrue();
        assertThat(result.toolCalls()).hasSize(1);
        assertThat(result.toolCalls().get(0).success()).isTrue();
        assertThat(result.toolCalls().get(0).arguments()).containsEntry("ticketNo", "INC-10002");
        ArgumentCaptor<TicketQueryRequest> requestCaptor = ArgumentCaptor.forClass(TicketQueryRequest.class);
        verify(ticketTools).queryTicket(requestCaptor.capture());
        assertThat(requestCaptor.getValue().ticketNo()).isEqualTo("INC-10002");
        assertThat(MDC.get(CorrelationContext.WORKFLOW_RUN_ID_KEY)).isNull();
        assertThat(MDC.get(CorrelationContext.WORKFLOW_STEP_ID_KEY)).isNull();
    }

    private AgentWorkflowState state(UUID runId) {
        return new AgentWorkflowState(Map.of(
                AgentWorkflowState.RUN_ID, runId,
                AgentWorkflowState.CONVERSATION_ID, UUID.randomUUID(),
                AgentWorkflowState.USER_ID, UUID.randomUUID(),
                AgentWorkflowState.TRACE_ID, "trace-workflow-tool",
                AgentWorkflowState.GOAL, "query ticket",
                AgentWorkflowState.CURRENT_STEP_INDEX, 0,
                AgentWorkflowState.STEP_RESULTS, List.of()));
    }
}
