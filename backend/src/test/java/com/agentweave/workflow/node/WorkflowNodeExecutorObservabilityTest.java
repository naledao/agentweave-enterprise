package com.agentweave.workflow.node;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agentweave.conversation.application.ConversationRagService;
import com.agentweave.conversation.application.ModelCallLogService;
import com.agentweave.conversation.domain.ModelCallStatus;
import com.agentweave.langchain4j.agent.AgentModelObservation;
import com.agentweave.langchain4j.agent.AgentPromptTemplateService;
import com.agentweave.langchain4j.agent.ExecutorAgent;
import com.agentweave.langchain4j.agent.PlannerAgent;
import com.agentweave.langchain4j.agent.ReviewerAgent;
import com.agentweave.shared.tracing.CorrelationContext;
import com.agentweave.tool.application.ToolDefinitionService;
import com.agentweave.tool.application.ToolRiskEvaluator;
import com.agentweave.workflow.application.AgentStepService;
import com.agentweave.workflow.application.PlanValidator;
import com.agentweave.workflow.application.WorkflowApprovalService;
import com.agentweave.workflow.application.WorkflowCheckpointService;
import com.agentweave.workflow.application.WorkflowRunService;
import com.agentweave.workflow.application.WorkflowToolExecutionService;
import com.agentweave.workflow.domain.AgentRunEntity;
import com.agentweave.workflow.domain.AgentStepEntity;
import com.agentweave.workflow.domain.AgentStepType;
import com.agentweave.workflow.domain.WorkflowRunStatus;
import com.agentweave.workflow.dto.AgentExecutionResult;
import com.agentweave.workflow.dto.WorkflowPlan;
import com.agentweave.workflow.dto.WorkflowPlanStep;
import com.agentweave.workflow.dto.WorkflowReviewResult;
import com.agentweave.workflow.graph.WorkflowNodeNames;
import com.agentweave.workflow.state.AgentWorkflowState;
import dev.langchain4j.model.output.TokenUsage;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WorkflowNodeExecutorObservabilityTest {

    private PlannerAgent plannerAgent;
    private ExecutorAgent executorAgent;
    private ReviewerAgent reviewerAgent;
    private WorkflowRunService workflowRunService;
    private AgentStepService agentStepService;
    private AgentPromptTemplateService promptTemplateService;
    private ModelCallLogService modelCallLogService;
    private AgentModelObservation agentModelObservation;
    private WorkflowToolExecutionService workflowToolExecutionService;
    private WorkflowNodeExecutor executor;
    private AgentRunEntity run;

    @BeforeEach
    void setUp() {
        plannerAgent = mock(PlannerAgent.class);
        executorAgent = mock(ExecutorAgent.class);
        reviewerAgent = mock(ReviewerAgent.class);
        workflowRunService = mock(WorkflowRunService.class);
        agentStepService = mock(AgentStepService.class);
        promptTemplateService = mock(AgentPromptTemplateService.class);
        modelCallLogService = mock(ModelCallLogService.class);
        agentModelObservation = mock(AgentModelObservation.class);
        workflowToolExecutionService = mock(WorkflowToolExecutionService.class);
        when(agentModelObservation.currentTokenUsage()).thenReturn(new TokenUsage(11, 7));

        executor = new WorkflowNodeExecutor(
                plannerAgent,
                executorAgent,
                reviewerAgent,
                mock(PlanValidator.class),
                workflowRunService,
                agentStepService,
                promptTemplateService,
                mock(ConversationRagService.class),
                mock(WorkflowCheckpointService.class),
                mock(ToolDefinitionService.class),
                new ToolRiskEvaluator(mock(ToolDefinitionService.class)),
                mock(WorkflowApprovalService.class),
                workflowToolExecutionService,
                modelCallLogService,
                agentModelObservation,
                new CorrelationContext(),
                "mimo-v2.5");

        run = new AgentRunEntity(UUID.randomUUID(), UUID.randomUUID(), "diagnose payment latency");
        run.setConversationId(UUID.randomUUID());
        run.setTraceId("trace-workflow-model-calls");
        when(workflowRunService.getEntityById(run.getId())).thenReturn(run);
    }

    @Test
    void plannerRecordsModelCallLog() {
        AgentStepEntity plannerStep = step(0, AgentStepType.PLANNING, WorkflowNodeNames.PLANNER);
        when(agentStepService.createStep(run, 0, AgentStepType.PLANNING, WorkflowNodeNames.PLANNER))
                .thenReturn(plannerStep);
        when(promptTemplateService.buildPlannerContext(run.getGoal(), null)).thenReturn("planner context");
        WorkflowPlan plan = WorkflowPlan.of(run.getGoal(), List.of(new WorkflowPlanStep(
                UUID.randomUUID(),
                0,
                AgentStepType.RAG_SEARCH,
                "find payment latency docs",
                List.of(),
                List.of(),
                "LOW",
                null,
                "HYBRID")));
        when(plannerAgent.createPlan(run.getGoal(), "planner context")).thenReturn(plan);

        executor.planner(state(Map.of()));

        verify(modelCallLogService).recordAgentCall(
                eq(run.getConversationId()),
                isNull(),
                eq("openai"),
                eq("mimo-v2.5"),
                any(),
                eq("steps=1"),
                eq(11),
                eq(7),
                any(Long.class),
                eq(ModelCallStatus.SUCCESS),
                isNull(),
                isNull(),
                eq(run.getTraceId()),
                eq("PLANNER"),
                eq(run.getId()),
                eq(plannerStep.getId()));
    }

    @Test
    void executorRecordsModelCallLog() {
        WorkflowPlanStep planStep = new WorkflowPlanStep(
                UUID.randomUUID(),
                0,
                AgentStepType.RAG_SEARCH,
                "find payment latency docs",
                List.of(),
                List.of(),
                "LOW",
                null,
                "HYBRID");
        AgentStepEntity persistedStep = step(1, AgentStepType.RAG_SEARCH, WorkflowNodeNames.RAG_NODE);
        when(agentStepService.nextStepIndex(run.getId())).thenReturn(1);
        when(agentStepService.createStep(run, 1, AgentStepType.RAG_SEARCH, WorkflowNodeNames.RAG_NODE))
                .thenReturn(persistedStep);
        when(promptTemplateService.formatStepResults(any())).thenReturn("");
        when(promptTemplateService.buildExecutorContext(planStep.instruction(), null, "HYBRID", ""))
                .thenReturn("executor context");
        when(executorAgent.executeStep(0, "RAG_SEARCH", planStep.instruction(), null, "HYBRID", "executor context"))
                .thenReturn(AgentExecutionResult.success("executor output", List.of(), List.of(), List.of()));

        executor.rag(state(Map.of(
                AgentWorkflowState.PLAN, WorkflowPlan.of(run.getGoal(), List.of(planStep)),
                AgentWorkflowState.CURRENT_STEP_INDEX, 0)));

        verify(modelCallLogService).recordAgentCall(
                eq(run.getConversationId()),
                isNull(),
                eq("openai"),
                eq("mimo-v2.5"),
                any(),
                eq("executor output"),
                eq(11),
                eq(7),
                any(Long.class),
                eq(ModelCallStatus.SUCCESS),
                isNull(),
                isNull(),
                eq(run.getTraceId()),
                eq("EXECUTOR"),
                eq(run.getId()),
                eq(persistedStep.getId()));
    }

    @Test
    void reviewerRecordsModelCallLog() {
        AgentStepEntity reviewerStep = step(2, AgentStepType.REVIEW, WorkflowNodeNames.REVIEWER);
        when(agentStepService.nextStepIndex(run.getId())).thenReturn(2);
        when(agentStepService.createStep(run, 2, AgentStepType.REVIEW, WorkflowNodeNames.REVIEWER))
                .thenReturn(reviewerStep);
        WorkflowReviewResult review = WorkflowReviewResult.sufficient("final answer", List.of(), List.of(), List.of());
        when(reviewerAgent.review(eq(run.getGoal()), any())).thenReturn(review);

        executor.reviewer(state(Map.of(AgentWorkflowState.STEP_RESULTS, List.of(
                AgentExecutionResult.success("executor output", List.of(), List.of(), List.of())))));

        verify(modelCallLogService).recordAgentCall(
                eq(run.getConversationId()),
                isNull(),
                eq("openai"),
                eq("mimo-v2.5"),
                any(),
                eq("Answer is sufficient"),
                eq(11),
                eq(7),
                any(Long.class),
                eq(ModelCallStatus.SUCCESS),
                isNull(),
                isNull(),
                eq(run.getTraceId()),
                eq("REVIEWER"),
                eq(run.getId()),
                eq(reviewerStep.getId()));
    }

    private AgentWorkflowState state(Map<String, Object> extra) {
        java.util.LinkedHashMap<String, Object> data = new java.util.LinkedHashMap<>();
        data.put(AgentWorkflowState.RUN_ID, run.getId());
        data.put(AgentWorkflowState.CONVERSATION_ID, run.getConversationId());
        data.put(AgentWorkflowState.USER_ID, run.getUserId());
        data.put(AgentWorkflowState.TRACE_ID, run.getTraceId());
        data.put(AgentWorkflowState.GOAL, run.getGoal());
        data.put(AgentWorkflowState.CURRENT_STEP_INDEX, 0);
        data.put(AgentWorkflowState.STEP_RESULTS, List.of());
        data.putAll(extra);
        return new AgentWorkflowState(data);
    }

    private AgentStepEntity step(int index, AgentStepType type, String nodeName) {
        return new AgentStepEntity(UUID.randomUUID(), run, index, type, nodeName);
    }
}
