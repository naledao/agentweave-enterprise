package com.agentweave.workflow.node;

import com.agentweave.conversation.application.ConversationPrompt;
import com.agentweave.conversation.application.ConversationRagService;
import com.agentweave.conversation.application.ModelCallLogService;
import com.agentweave.conversation.application.RagPromptContext;
import com.agentweave.conversation.domain.ModelCallStatus;
import com.agentweave.graphrag.dto.GraphPathResponse;
import com.agentweave.langchain4j.agent.AgentModelObservation;
import com.agentweave.langchain4j.agent.AgentPromptTemplateService;
import com.agentweave.langchain4j.agent.ExecutorAgent;
import com.agentweave.langchain4j.agent.PlannerAgent;
import com.agentweave.langchain4j.agent.ReviewerAgent;
import com.agentweave.tool.application.ToolDefinitionService;
import com.agentweave.tool.application.ToolRiskEvaluator;
import com.agentweave.tool.domain.ToolDefinitionEntity;
import com.agentweave.tool.domain.ToolRiskLevel;
import com.agentweave.shared.tracing.CorrelationContext;
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
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class WorkflowNodeExecutor {

    private static final Logger log = LoggerFactory.getLogger(WorkflowNodeExecutor.class);

    private final PlannerAgent plannerAgent;
    private final ExecutorAgent executorAgent;
    private final ReviewerAgent reviewerAgent;
    private final PlanValidator planValidator;
    private final WorkflowRunService workflowRunService;
    private final AgentStepService agentStepService;
    private final AgentPromptTemplateService promptTemplateService;
    private final ConversationRagService conversationRagService;
    private final WorkflowCheckpointService checkpointService;
    private final ToolDefinitionService toolDefinitionService;
    private final ToolRiskEvaluator toolRiskEvaluator;
    private final WorkflowApprovalService approvalService;
    private final WorkflowToolExecutionService workflowToolExecutionService;
    private final ModelCallLogService modelCallLogService;
    private final AgentModelObservation agentModelObservation;
    private final CorrelationContext correlationContext;
    private final String agentProvider;
    private final String agentModelName;

    public WorkflowNodeExecutor(
            PlannerAgent plannerAgent,
            ExecutorAgent executorAgent,
            ReviewerAgent reviewerAgent,
            PlanValidator planValidator,
            WorkflowRunService workflowRunService,
            AgentStepService agentStepService,
            AgentPromptTemplateService promptTemplateService,
            ConversationRagService conversationRagService,
            WorkflowCheckpointService checkpointService,
            ToolDefinitionService toolDefinitionService,
            ToolRiskEvaluator toolRiskEvaluator,
            WorkflowApprovalService approvalService,
            WorkflowToolExecutionService workflowToolExecutionService,
            ModelCallLogService modelCallLogService,
            AgentModelObservation agentModelObservation,
            CorrelationContext correlationContext,
            @Value("${langchain4j.open-ai.chat-model.model-name:unknown}") String agentModelName) {
        this.plannerAgent = plannerAgent;
        this.executorAgent = executorAgent;
        this.reviewerAgent = reviewerAgent;
        this.planValidator = planValidator;
        this.workflowRunService = workflowRunService;
        this.agentStepService = agentStepService;
        this.promptTemplateService = promptTemplateService;
        this.conversationRagService = conversationRagService;
        this.checkpointService = checkpointService;
        this.toolDefinitionService = toolDefinitionService;
        this.toolRiskEvaluator = toolRiskEvaluator;
        this.approvalService = approvalService;
        this.workflowToolExecutionService = workflowToolExecutionService;
        this.modelCallLogService = modelCallLogService;
        this.agentModelObservation = agentModelObservation;
        this.correlationContext = correlationContext;
        this.agentProvider = "openai";
        this.agentModelName = agentModelName;
    }

    public Map<String, Object> loadContext(AgentWorkflowState state) {
        checkpointService.save(WorkflowNodeNames.LOAD_CONTEXT, state);
        return Map.of();
    }

    public Map<String, Object> planner(AgentWorkflowState state) {
        AgentStepEntity step = null;
        try {
            transitionIfNeeded(state.runId(), WorkflowRunStatus.PLANNING);
            AgentRunEntity run = workflowRunService.getEntityById(state.runId());
            step = createAndStartStep(run, 0, AgentStepType.PLANNING, WorkflowNodeNames.PLANNER);
            agentStepService.recordInputSummary(step.getId(), "goal=" + run.getGoal());

            String context = promptTemplateService.buildPlannerContext(run.getGoal(), null);
            WorkflowPlan plan = callPlannerAgent(run, step, context);
            agentStepService.completeStep(step.getId(), "Plan created with " + plan.steps().size() + " steps");

            Map<String, Object> updates = Map.of(AgentWorkflowState.PLAN, plan);
            checkpointService.save(WorkflowNodeNames.PLANNER, updated(state, updates));
            return updates;
        } catch (Exception ex) {
            failStepIfStarted(step, "PLANNING_FAILED", ex.getMessage());
            return fail(state, WorkflowNodeNames.PLANNER, null, "PLANNING_FAILED", ex, false);
        }
    }

    public Map<String, Object> validatePlan(AgentWorkflowState state) {
        try {
            PlanValidator.ValidationResult result = planValidator.validate(state.plan());
            if (!result.passed()) {
                return fail(state, WorkflowNodeNames.VALIDATE_PLAN, null, "INVALID_PLAN", result.message(), false);
            }
            checkpointService.save(WorkflowNodeNames.VALIDATE_PLAN, state);
            return Map.of();
        } catch (Exception ex) {
            return fail(state, WorkflowNodeNames.VALIDATE_PLAN, null, "PLAN_VALIDATION_FAILED", ex, false);
        }
    }

    public Map<String, Object> routeStep(AgentWorkflowState state) {
        String nextNode = route(state);
        Map<String, Object> updates = Map.of(AgentWorkflowState.NEXT_NODE, nextNode);
        checkpointService.save(WorkflowNodeNames.ROUTE_STEP, updated(state, updates));
        return updates;
    }

    public String nextNode(AgentWorkflowState state) {
        return state.value(AgentWorkflowState.NEXT_NODE, WorkflowNodeNames.REVIEWER);
    }

    public Map<String, Object> rag(AgentWorkflowState state) {
        WorkflowPlanStep step = currentStep(state);
        return executeStep(state, step, WorkflowNodeNames.RAG_NODE, AgentStepType.RAG_SEARCH, stepEntity -> {
            AgentExecutionResult agentResult = executeWithAgent(state, step, stepEntity);
            RagPromptContext ragContext = conversationRagService.retrieve(new ConversationPrompt(
                    state.conversationId(),
                    "Workflow " + state.runId(),
                    step.instruction(),
                    List.of()));
            List<WorkflowReviewResult.Citation> citations = mergeCitations(agentResult, ragContext);
            List<WorkflowReviewResult.GraphPath> graphPaths = mergeGraphPaths(agentResult, ragContext);
            return new AgentExecutionResult(
                    true,
                    outputSummary(agentResult, ragContext.promptContext()),
                    citations,
                    graphPaths,
                    safeToolCalls(agentResult),
                    null,
                    Map.of("retrievalMode", ragContext.retrievalMode()));
        });
    }

    public Map<String, Object> graphRag(AgentWorkflowState state) {
        WorkflowPlanStep step = currentStep(state);
        return executeStep(state, step, WorkflowNodeNames.GRAPH_RAG_NODE, AgentStepType.GRAPH_RAG_SEARCH, stepEntity -> {
            AgentExecutionResult agentResult = executeWithAgent(state, step, stepEntity);
            RagPromptContext ragContext = conversationRagService.retrieve(new ConversationPrompt(
                    state.conversationId(),
                    "Workflow " + state.runId(),
                    step.instruction(),
                    List.of()));
            return new AgentExecutionResult(
                    true,
                    outputSummary(agentResult, ragContext.promptContext()),
                    mergeCitations(agentResult, ragContext),
                    mergeGraphPaths(agentResult, ragContext),
                    safeToolCalls(agentResult),
                    null,
                    Map.of("retrievalMode", ragContext.retrievalMode()));
        });
    }

    public Map<String, Object> tool(AgentWorkflowState state) {
        WorkflowPlanStep step = currentStep(state);
        return executeStep(state, step, WorkflowNodeNames.TOOL_NODE, AgentStepType.TOOL_CALL,
                stepEntity -> executeTool(state, step, stepEntity));
    }

    public Map<String, Object> humanApproval(AgentWorkflowState state) {
        WorkflowPlanStep step = currentStep(state);
        AgentStepEntity stepEntity = null;
        try {
            transitionToExecuting(state.runId());
            AgentRunEntity run = workflowRunService.getEntityById(state.runId());
            stepEntity = createAndStartStep(run, nextPersistedStepIndex(run), AgentStepType.HUMAN_APPROVAL, WorkflowNodeNames.HUMAN_APPROVAL_NODE);
            agentStepService.recordInputSummary(stepEntity.getId(), inputSummary(step));
            ToolRiskLevel riskLevel = toolRiskEvaluator.evaluate(step);
            var approval = approvalService.createPendingApproval(run, stepEntity, step, riskLevel);
            String summary = "Approval pending for high-risk tool " + nullSafe(step.toolCode());

            Map<String, Object> updates = Map.of(
                    AgentWorkflowState.RISK_LEVEL, riskLevel.name(),
                    AgentWorkflowState.APPROVAL_STATUS, approval.getStatus().name(),
                    AgentWorkflowState.APPROVAL_ID, approval.getId());
            checkpointService.save(WorkflowNodeNames.HUMAN_APPROVAL_NODE, updated(state, updates));
            return updates;
        } catch (Exception ex) {
            failStepIfStarted(stepEntity, "APPROVAL_NODE_FAILED", ex.getMessage());
            return fail(state, WorkflowNodeNames.HUMAN_APPROVAL_NODE, step, "APPROVAL_NODE_FAILED", ex, true);
        }
    }

    public Map<String, Object> reviewer(AgentWorkflowState state) {
        AgentStepEntity step = null;
        try {
            transitionToReviewing(state.runId());
            AgentRunEntity run = workflowRunService.getEntityById(state.runId());
            step = createAndStartStep(run, nextPersistedStepIndex(run), AgentStepType.REVIEW, WorkflowNodeNames.REVIEWER);
            agentStepService.recordInputSummary(step.getId(),
                    "goal=" + run.getGoal() + "; stepResults=" + state.stepResults().size());
            String stepResults = formatStepResultsForReview(state.stepResults());
            WorkflowReviewResult reviewResult = callReviewerAgent(run, step, stepResults);
            agentStepService.completeStep(step.getId(), reviewResult.summary());

            String finalAnswer = reviewResult.finalAnswer() == null || reviewResult.finalAnswer().isBlank()
                    ? reviewResult.summary()
                    : reviewResult.finalAnswer();
            Map<String, Object> updates = Map.of(
                    AgentWorkflowState.FINAL_ANSWER, finalAnswer,
                    AgentWorkflowState.CITATIONS, reviewResult.citations(),
                    AgentWorkflowState.GRAPH_PATHS, reviewResult.graphPaths(),
                    AgentWorkflowState.TOOL_CALLS, reviewResult.toolCalls());
            checkpointService.save(WorkflowNodeNames.REVIEWER, updated(state, updates));
            return updates;
        } catch (Exception ex) {
            failStepIfStarted(step, "REVIEW_FAILED", ex.getMessage());
            return fail(state, WorkflowNodeNames.REVIEWER, null, "REVIEW_FAILED", ex, true);
        }
    }

    public Map<String, Object> persistResult(AgentWorkflowState state) {
        if (state.hasError()) {
            return error(state);
        }
        workflowRunService.markSucceeded(state.runId(), state.finalAnswer());
        checkpointService.save(WorkflowNodeNames.PERSIST_RESULT, state);
        return Map.of();
    }

    public Map<String, Object> error(AgentWorkflowState state) {
        AgentWorkflowState.WorkflowError error = state.error();
        String code = error == null ? "WORKFLOW_ERROR" : error.code();
        String message = error == null ? "Workflow execution failed" : error.message();
        try {
            workflowRunService.markFailed(state.runId(), code, message);
            checkpointService.save(WorkflowNodeNames.ERROR, state);
        } catch (RuntimeException ex) {
            log.warn("Workflow error persistence failed: runId={}, error={}", state.runId(), ex.getMessage(), ex);
        }
        return Map.of();
    }

    private Map<String, Object> executeStep(
            AgentWorkflowState state,
            WorkflowPlanStep step,
            String nodeName,
            AgentStepType stepType,
            StepCallable callable) {
        AgentStepEntity stepEntity = null;
        try {
            transitionToExecuting(state.runId());
            AgentRunEntity run = workflowRunService.getEntityById(state.runId());
            stepEntity = createAndStartStep(run, nextPersistedStepIndex(run), stepType, nodeName);
            agentStepService.recordInputSummary(stepEntity.getId(), inputSummary(step));
            AgentExecutionResult result;
            try (CorrelationContext.Scope ignored = correlationContext.openWorkflow(
                    run.getTraceId(),
                    run.getConversationId(),
                    null,
                    run.getId(),
                    stepEntity.getId())) {
                result = callable.execute(stepEntity);
            }
            if (!result.success()) {
                agentStepService.failStep(stepEntity.getId(), "STEP_EXECUTION_FAILED", result.errorMessage());
                return fail(state, nodeName, step, "STEP_EXECUTION_FAILED", result.errorMessage(), true);
            }
            agentStepService.completeStep(
                    stepEntity.getId(),
                    result.outputSummary(),
                    safeCitations(result),
                    safeGraphPaths(result),
                    safeToolCalls(result));
            Map<String, Object> updates = stepUpdates(state, result, null);
            checkpointService.save(nodeName, updated(state, updates));
            return updates;
        } catch (Exception ex) {
            failStepIfStarted(stepEntity, "STEP_EXECUTION_ERROR", ex.getMessage());
            return fail(state, nodeName, step, "STEP_EXECUTION_ERROR", ex, true);
        }
    }

    private void failStepIfStarted(AgentStepEntity step, String errorCode, String errorMessage) {
        if (step == null) {
            return;
        }
        try {
            agentStepService.failStep(step.getId(), errorCode, errorMessage);
        } catch (RuntimeException failure) {
            log.warn(
                    "Workflow step failure persistence failed: stepId={}, error={}",
                    step.getId(),
                    failure.getMessage(),
                    failure);
        }
    }

    private AgentExecutionResult executeWithAgent(
            AgentWorkflowState state,
            WorkflowPlanStep step,
            AgentStepEntity stepEntity) {
        String context = promptTemplateService.buildExecutorContext(
                step.instruction(),
                step.toolCode(),
                step.retrievalMode(),
                promptTemplateService.formatStepResults(previousResultMap(state.stepResults())));
        AgentRunEntity run = workflowRunService.getEntityById(state.runId());
        return callExecutorAgent(run, stepEntity, step, context);
    }

    private WorkflowPlan callPlannerAgent(AgentRunEntity run, AgentStepEntity step, String context) {
        long startedAt = System.nanoTime();
        String promptSummary = "goal=" + run.getGoal() + ";context=" + context;
        try {
            WorkflowPlan plan = plannerAgent.createPlan(run.getGoal(), context);
            TokenUsage tokenUsage = currentTokenUsage();
            modelCallLogService.recordAgentCall(
                    run.getConversationId(),
                    null,
                    agentProvider,
                    agentModelName,
                    promptSummary,
                    "steps=" + plan.steps().size(),
                    inputTokens(tokenUsage),
                    outputTokens(tokenUsage),
                    elapsedMillis(startedAt),
                    ModelCallStatus.SUCCESS,
                    null,
                    null,
                    run.getTraceId(),
                    "PLANNER",
                    run.getId(),
                    step.getId());
            return plan;
        } catch (RuntimeException ex) {
            recordAgentFailure(run, step, "PLANNER", promptSummary, startedAt, "PLANNER_FAILED", ex);
            throw ex;
        } finally {
            agentModelObservation.clear();
        }
    }

    private AgentExecutionResult callExecutorAgent(
            AgentRunEntity run,
            AgentStepEntity stepEntity,
            WorkflowPlanStep step,
            String context) {
        long startedAt = System.nanoTime();
        String promptSummary = "stepIndex=" + step.stepIndex()
                + ";stepType=" + step.stepType()
                + ";instruction=" + nullSafe(step.instruction())
                + ";toolCode=" + nullSafe(step.toolCode())
                + ";retrievalMode=" + nullSafe(step.retrievalMode())
                + ";context=" + context;
        try {
            AgentExecutionResult result = executorAgent.executeStep(
                    step.stepIndex(),
                    step.stepType().name(),
                    step.instruction(),
                    step.toolCode(),
                    step.retrievalMode(),
                    context);
            TokenUsage tokenUsage = currentTokenUsage();
            modelCallLogService.recordAgentCall(
                    run.getConversationId(),
                    null,
                    agentProvider,
                    agentModelName,
                    promptSummary,
                    result == null ? null : result.outputSummary(),
                    inputTokens(tokenUsage),
                    outputTokens(tokenUsage),
                    elapsedMillis(startedAt),
                    ModelCallStatus.SUCCESS,
                    null,
                    null,
                    run.getTraceId(),
                    "EXECUTOR",
                    run.getId(),
                    stepEntity.getId());
            return result;
        } catch (RuntimeException ex) {
            recordAgentFailure(run, stepEntity, "EXECUTOR", promptSummary, startedAt, "EXECUTOR_FAILED", ex);
            throw ex;
        } finally {
            agentModelObservation.clear();
        }
    }

    private WorkflowReviewResult callReviewerAgent(AgentRunEntity run, AgentStepEntity step, String stepResults) {
        long startedAt = System.nanoTime();
        String promptSummary = "goal=" + run.getGoal() + ";stepResults=" + stepResults;
        try {
            WorkflowReviewResult result = reviewerAgent.review(run.getGoal(), stepResults);
            TokenUsage tokenUsage = currentTokenUsage();
            modelCallLogService.recordAgentCall(
                    run.getConversationId(),
                    null,
                    agentProvider,
                    agentModelName,
                    promptSummary,
                    result == null ? null : result.summary(),
                    inputTokens(tokenUsage),
                    outputTokens(tokenUsage),
                    elapsedMillis(startedAt),
                    ModelCallStatus.SUCCESS,
                    null,
                    null,
                    run.getTraceId(),
                    "REVIEWER",
                    run.getId(),
                    step.getId());
            return result;
        } catch (RuntimeException ex) {
            recordAgentFailure(run, step, "REVIEWER", promptSummary, startedAt, "REVIEWER_FAILED", ex);
            throw ex;
        } finally {
            agentModelObservation.clear();
        }
    }

    private void recordAgentFailure(
            AgentRunEntity run,
            AgentStepEntity step,
            String agentStage,
            String promptSummary,
            long startedAt,
            String errorCode,
            RuntimeException ex) {
        modelCallLogService.recordAgentCall(
                run.getConversationId(),
                null,
                agentProvider,
                agentModelName,
                promptSummary,
                null,
                null,
                null,
                elapsedMillis(startedAt),
                ModelCallStatus.FAILED,
                errorCode,
                ex.getMessage(),
                run.getTraceId(),
                agentStage,
                run.getId(),
                step == null ? null : step.getId());
    }

    private TokenUsage currentTokenUsage() {
        return agentModelObservation.currentTokenUsage();
    }

    private Integer inputTokens(TokenUsage tokenUsage) {
        return tokenUsage == null ? null : tokenUsage.inputTokenCount();
    }

    private Integer outputTokens(TokenUsage tokenUsage) {
        return tokenUsage == null ? null : tokenUsage.outputTokenCount();
    }

    private long elapsedMillis(long startedAt) {
        return Duration.ofNanos(System.nanoTime() - startedAt).toMillis();
    }

    private Map<String, Object> stepUpdates(AgentWorkflowState state, AgentExecutionResult result, String approvalStatus) {
        Map<String, Object> updates = new LinkedHashMap<>();
        int nextStepIndex = state.currentStepIndex() + 1;
        workflowRunService.updateCurrentStepIndex(state.runId(), nextStepIndex);
        updates.put(AgentWorkflowState.CURRENT_STEP_INDEX, nextStepIndex);
        updates.put(AgentWorkflowState.STEP_RESULTS, List.of(result));
        updates.put(AgentWorkflowState.CITATIONS, safeCitations(result));
        updates.put(AgentWorkflowState.GRAPH_PATHS, safeGraphPaths(result));
        updates.put(AgentWorkflowState.TOOL_CALLS, safeToolCalls(result));
        if (approvalStatus != null) {
            updates.put(AgentWorkflowState.APPROVAL_STATUS, approvalStatus);
        } else {
            updates.put(AgentWorkflowState.APPROVAL_STATUS, "");
            updates.put(AgentWorkflowState.APPROVAL_ID, "");
        }
        return updates;
    }

    private String route(AgentWorkflowState state) {
        if (state.hasError()) {
            return WorkflowNodeNames.ERROR;
        }
            WorkflowPlan plan = state.plan();
        if (plan == null || state.currentStepIndex() >= plan.steps().size()) {
            return WorkflowNodeNames.REVIEWER;
        }
        WorkflowPlanStep step = currentStep(state);
        if (step.stepType() == AgentStepType.RAG_SEARCH) {
            return WorkflowNodeNames.RAG_NODE;
        }
        if (step.stepType() == AgentStepType.GRAPH_RAG_SEARCH) {
            return WorkflowNodeNames.GRAPH_RAG_NODE;
        }
        if (step.stepType() == AgentStepType.TOOL_CALL) {
            if (toolRiskEvaluator.requiresApproval(step)
                    && !"APPROVED".equalsIgnoreCase(state.approvalStatus())) {
                return WorkflowNodeNames.HUMAN_APPROVAL_NODE;
            }
            return WorkflowNodeNames.TOOL_NODE;
        }
        if (step.stepType() == AgentStepType.HUMAN_APPROVAL) {
            return WorkflowNodeNames.HUMAN_APPROVAL_NODE;
        }
        return WorkflowNodeNames.REVIEWER;
    }

    private WorkflowPlanStep currentStep(AgentWorkflowState state) {
        WorkflowPlan plan = state.plan();
        if (plan == null || state.currentStepIndex() >= plan.steps().size()) {
            throw new IllegalStateException("No workflow step available at index " + state.currentStepIndex());
        }
        return plan.steps().get(state.currentStepIndex());
    }

    private AgentExecutionResult executeTool(AgentWorkflowState state, WorkflowPlanStep step, AgentStepEntity stepEntity) {
        String toolCode = nullSafe(step.toolCode()).toLowerCase();
        ToolDefinitionEntity definition = step.toolCode() == null
                ? null
                : toolDefinitionService.findByPermissionCode(step.toolCode()).orElse(null);
        if (definition != null
                && definition.getRiskLevel() == ToolRiskLevel.HIGH
                && !"APPROVED".equalsIgnoreCase(state.approvalStatus())) {
            return AgentExecutionResult.failure("high risk tool requires approval");
        }
        if (!List.of("tool:ticket:query", "tool:log:search", "tool:api-status:query").contains(toolCode)) {
            return AgentExecutionResult.failure("Unsupported tool code: " + step.toolCode());
        }
        return workflowToolExecutionService.execute(state, step, stepEntity);
    }

    private AgentStepEntity createAndStartStep(AgentRunEntity run, int stepIndex, AgentStepType stepType, String nodeName) {
        AgentStepEntity step = agentStepService.createStep(run, stepIndex, stepType, nodeName);
        agentStepService.startStep(step.getId());
        return step;
    }

    private String inputSummary(WorkflowPlanStep step) {
        if (step == null) {
            return null;
        }
        StringBuilder summary = new StringBuilder();
        summary.append("instruction=").append(nullSafe(step.instruction()));
        if (step.toolCode() != null && !step.toolCode().isBlank()) {
            summary.append("; toolCode=").append(step.toolCode());
        }
        if (step.retrievalMode() != null && !step.retrievalMode().isBlank()) {
            summary.append("; retrievalMode=").append(step.retrievalMode());
        }
        return summary.toString();
    }

    private void transitionToExecuting(UUID runId) {
        AgentRunEntity run = workflowRunService.getEntityById(runId);
        if (run.getStatus() == WorkflowRunStatus.EXECUTING) {
            return;
        }
        if (run.getStatus() == WorkflowRunStatus.WAITING_APPROVAL) {
            transitionIfNeeded(runId, WorkflowRunStatus.EXECUTING);
            return;
        }
        if (run.getStatus() == WorkflowRunStatus.CREATED) {
            workflowRunService.transitionTo(runId, WorkflowRunStatus.PLANNING);
        }
        transitionIfNeeded(runId, WorkflowRunStatus.EXECUTING);
    }

    private void transitionToReviewing(UUID runId) {
        AgentRunEntity run = workflowRunService.getEntityById(runId);
        if (run.getStatus() == WorkflowRunStatus.REVIEWING) {
            return;
        }
        if (run.getStatus() == WorkflowRunStatus.CREATED) {
            workflowRunService.transitionTo(runId, WorkflowRunStatus.PLANNING);
            workflowRunService.transitionTo(runId, WorkflowRunStatus.EXECUTING);
        } else if (run.getStatus() == WorkflowRunStatus.PLANNING) {
            workflowRunService.transitionTo(runId, WorkflowRunStatus.EXECUTING);
        } else if (run.getStatus() == WorkflowRunStatus.WAITING_APPROVAL) {
            workflowRunService.transitionTo(runId, WorkflowRunStatus.EXECUTING);
        }
        transitionIfNeeded(runId, WorkflowRunStatus.REVIEWING);
    }

    private void transitionIfNeeded(UUID runId, WorkflowRunStatus targetStatus) {
        AgentRunEntity run = workflowRunService.getEntityById(runId);
        if (run.getStatus() != targetStatus) {
            workflowRunService.transitionTo(runId, targetStatus);
        }
    }

    private Map<String, Object> fail(
            AgentWorkflowState state,
            String nodeName,
            WorkflowPlanStep step,
            String code,
            Exception ex,
            boolean recoverable) {
        return fail(state, nodeName, step, code, ex.getMessage(), recoverable);
    }

    private Map<String, Object> fail(
            AgentWorkflowState state,
            String nodeName,
            WorkflowPlanStep step,
            String code,
            String message,
            boolean recoverable) {
        AgentWorkflowState.WorkflowError error = new AgentWorkflowState.WorkflowError(
                code,
                message == null || message.isBlank() ? code : message,
                nodeName,
                step == null ? null : step.stepIndex(),
                recoverable);
        Map<String, Object> updates = Map.of(
                AgentWorkflowState.ERROR, error,
                AgentWorkflowState.NEXT_NODE, WorkflowNodeNames.ERROR);
        checkpointService.save(nodeName, updated(state, updates));
        return updates;
    }

    private AgentWorkflowState updated(AgentWorkflowState state, Map<String, Object> updates) {
        Map<String, Object> data = new LinkedHashMap<>(state.data());
        updates.forEach((key, value) -> {
            if (value instanceof List<?> newItems && data.get(key) instanceof List<?> existing) {
                List<Object> merged = new ArrayList<>(existing);
                merged.addAll(newItems);
                data.put(key, merged);
            } else {
                data.put(key, value);
            }
        });
        return new AgentWorkflowState(data);
    }

    private Map<Integer, Object> previousResultMap(List<AgentExecutionResult> results) {
        Map<Integer, Object> previous = new LinkedHashMap<>();
        for (int i = 0; i < results.size(); i++) {
            previous.put(i, results.get(i));
        }
        return previous;
    }

    private List<WorkflowReviewResult.Citation> mergeCitations(AgentExecutionResult result, RagPromptContext context) {
        List<WorkflowReviewResult.Citation> citations = new ArrayList<>(safeCitations(result));
        citations.addAll(context.citations().stream()
                .map(citation -> new WorkflowReviewResult.Citation(
                        citation.documentId(),
                        citation.chunkId(),
                        citation.snippet(),
                        citation.score(),
                        citation.source()))
                .toList());
        return citations;
    }

    private List<WorkflowReviewResult.GraphPath> mergeGraphPaths(AgentExecutionResult result, RagPromptContext context) {
        List<WorkflowReviewResult.GraphPath> graphPaths = new ArrayList<>(safeGraphPaths(result));
        graphPaths.addAll(context.graphPaths().stream()
                .map(this::toWorkflowGraphPath)
                .toList());
        return graphPaths;
    }

    private WorkflowReviewResult.GraphPath toWorkflowGraphPath(GraphPathResponse path) {
        return new WorkflowReviewResult.GraphPath(
                path.entities(),
                path.relationships(),
                String.join(" -> ", path.entities()));
    }

    private List<WorkflowReviewResult.Citation> safeCitations(AgentExecutionResult result) {
        return result == null || result.citations() == null ? List.of() : result.citations();
    }

    private List<WorkflowReviewResult.GraphPath> safeGraphPaths(AgentExecutionResult result) {
        return result == null || result.graphPaths() == null ? List.of() : result.graphPaths();
    }

    private List<WorkflowReviewResult.ToolCallResult> safeToolCalls(AgentExecutionResult result) {
        return result == null || result.toolCalls() == null ? List.of() : result.toolCalls();
    }

    private String outputSummary(AgentExecutionResult result, String promptContext) {
        if (result != null && result.outputSummary() != null && !result.outputSummary().isBlank()) {
            return result.outputSummary();
        }
        if (promptContext != null && !promptContext.isBlank()) {
            return promptContext.length() > 1000 ? promptContext.substring(0, 1000) : promptContext;
        }
        return "Step completed";
    }

    private String formatStepResultsForReview(List<AgentExecutionResult> stepResults) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < stepResults.size(); i++) {
            AgentExecutionResult result = stepResults.get(i);
            sb.append("Step ").append(i).append(": ").append(result.outputSummary()).append("\n");
            if (!safeCitations(result).isEmpty()) {
                sb.append("  Citations: ").append(safeCitations(result).size()).append(" documents\n");
            }
            if (!safeGraphPaths(result).isEmpty()) {
                sb.append("  Graph paths: ").append(safeGraphPaths(result).size()).append(" paths\n");
            }
            if (!safeToolCalls(result).isEmpty()) {
                sb.append("  Tool calls: ").append(safeToolCalls(result).size()).append(" calls\n");
            }
        }
        return sb.toString();
    }

    private int nextPersistedStepIndex(AgentRunEntity run) {
        return agentStepService.nextStepIndex(run.getId());
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    @FunctionalInterface
    private interface StepCallable {
        AgentExecutionResult execute(AgentStepEntity stepEntity);
    }
}
