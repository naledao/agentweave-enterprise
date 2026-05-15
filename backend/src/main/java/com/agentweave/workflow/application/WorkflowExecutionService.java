package com.agentweave.workflow.application;

import com.agentweave.workflow.domain.AgentRunEntity;
import com.agentweave.workflow.dto.WorkflowRunResponse;
import com.agentweave.workflow.graph.AgentWorkflowGraph;
import com.agentweave.workflow.state.AgentWorkflowState;
import com.agentweave.shared.exception.BusinessException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.bsc.langgraph4j.GraphInput;
import org.bsc.langgraph4j.RunnableConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class WorkflowExecutionService {

    private static final Logger log = LoggerFactory.getLogger(WorkflowExecutionService.class);

    private final AgentWorkflowGraph agentWorkflowGraph;
    private final WorkflowRunService workflowRunService;
    private final WorkflowRecoveryService workflowRecoveryService;
    private final WorkflowRetryService workflowRetryService;

    public WorkflowExecutionService(
            AgentWorkflowGraph agentWorkflowGraph,
            WorkflowRunService workflowRunService,
            WorkflowRecoveryService workflowRecoveryService,
            WorkflowRetryService workflowRetryService) {
        this.agentWorkflowGraph = agentWorkflowGraph;
        this.workflowRunService = workflowRunService;
        this.workflowRecoveryService = workflowRecoveryService;
        this.workflowRetryService = workflowRetryService;
    }

    public WorkflowRunResponse executeWorkflow(UUID runId) {
        AgentRunEntity run = workflowRunService.getEntityById(runId);
        Map<String, Object> input = initialState(run);
        RunnableConfig config = runnableConfig(runId);
        try {
            agentWorkflowGraph.compiledGraph().invoke(GraphInput.args(input), config);
            return workflowRunService.getInternal(runId);
        } catch (Exception ex) {
            log.error("Workflow graph execution failed for run {}: {}", runId, ex.getMessage(), ex);
            workflowRunService.markFailed(runId, "WORKFLOW_GRAPH_ERROR", ex.getMessage());
            throw new WorkflowException("Workflow graph execution failed", ex);
        }
    }

    public WorkflowRunResponse resumeWorkflow(UUID runId) {
        RunnableConfig config = runnableConfig(runId);
        try {
            AgentWorkflowState state = workflowRecoveryService.prepareResume(runId);
            RunnableConfig resumeConfig = agentWorkflowGraph.compiledGraph()
                    .updateState(config, state.data(), workflowRecoveryService.recoveryStartNode(state));
            agentWorkflowGraph.compiledGraph().invoke(GraphInput.resume(), resumeConfig);
            return workflowRunService.getInternal(runId);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Workflow resume failed for run {}: {}", runId, ex.getMessage(), ex);
            workflowRunService.markFailed(runId, "WORKFLOW_RESUME_ERROR", ex.getMessage());
            throw new WorkflowException("Workflow resume failed", ex);
        }
    }

    public WorkflowRunResponse resumeAfterApproval(UUID runId, UUID approvalId) {
        RunnableConfig config = runnableConfig(runId);
        try {
            AgentWorkflowState state = workflowRecoveryService.prepareApprovalResume(runId, approvalId);
            RunnableConfig resumeConfig = agentWorkflowGraph.compiledGraph()
                    .updateState(config, state.data(), workflowRecoveryService.approvalResumeStartNode());
            agentWorkflowGraph.compiledGraph().invoke(GraphInput.resume(), resumeConfig);
            return workflowRunService.getInternal(runId);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Workflow approval resume failed for run {}: {}", runId, ex.getMessage(), ex);
            workflowRunService.markFailed(runId, "WORKFLOW_APPROVAL_RESUME_ERROR", ex.getMessage());
            throw new WorkflowException("Workflow approval resume failed", ex);
        }
    }

    public WorkflowRunResponse retryWorkflow(UUID runId, com.agentweave.workflow.dto.WorkflowRetryRequest request) {
        RunnableConfig config = runnableConfig(runId);
        try {
            AgentWorkflowState state = workflowRetryService.prepareRetry(runId, request);
            RunnableConfig resumeConfig = agentWorkflowGraph.compiledGraph()
                    .updateState(config, state.data(), workflowRecoveryService.recoveryStartNode(state));
            agentWorkflowGraph.compiledGraph().invoke(GraphInput.resume(), resumeConfig);
            return workflowRunService.getInternal(runId);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Workflow retry failed for run {}: {}", runId, ex.getMessage(), ex);
            workflowRunService.markFailed(runId, "WORKFLOW_RETRY_ERROR", ex.getMessage());
            throw new WorkflowException("Workflow retry failed", ex);
        }
    }

    private Map<String, Object> initialState(AgentRunEntity run) {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put(AgentWorkflowState.RUN_ID, run.getId());
        state.put(AgentWorkflowState.CONVERSATION_ID, run.getConversationId());
        state.put(AgentWorkflowState.USER_ID, run.getUserId());
        state.put(AgentWorkflowState.TRACE_ID, run.getTraceId());
        state.put(AgentWorkflowState.GOAL, run.getGoal());
        state.put(AgentWorkflowState.CURRENT_STEP_INDEX, 0);
        state.put(AgentWorkflowState.STEP_RESULTS, java.util.List.of());
        state.put(AgentWorkflowState.CITATIONS, java.util.List.of());
        state.put(AgentWorkflowState.GRAPH_PATHS, java.util.List.of());
        state.put(AgentWorkflowState.TOOL_CALLS, java.util.List.of());
        return state;
    }

    private RunnableConfig runnableConfig(UUID runId) {
        return RunnableConfig.builder()
                .threadId(runId.toString())
                .build();
    }

    public static class WorkflowException extends RuntimeException {
        public WorkflowException(String message) {
            super(message);
        }

        public WorkflowException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
