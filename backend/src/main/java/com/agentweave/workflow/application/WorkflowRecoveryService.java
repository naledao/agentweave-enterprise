package com.agentweave.workflow.application;

import com.agentweave.shared.exception.BusinessException;
import com.agentweave.shared.exception.ErrorCode;
import com.agentweave.workflow.domain.AgentRunEntity;
import com.agentweave.workflow.domain.WorkflowCheckpointEntity;
import com.agentweave.workflow.domain.WorkflowRunStatus;
import com.agentweave.workflow.dto.WorkflowCheckpointResponse;
import com.agentweave.workflow.state.AgentWorkflowState;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkflowRecoveryService {

    private final WorkflowRunService workflowRunService;
    private final WorkflowCheckpointService checkpointService;

    public WorkflowRecoveryService(
            WorkflowRunService workflowRunService,
            WorkflowCheckpointService checkpointService) {
        this.workflowRunService = workflowRunService;
        this.checkpointService = checkpointService;
    }

    @Transactional(readOnly = true)
    public WorkflowCheckpointResponse latestCheckpoint(UUID runId) {
        workflowRunService.getAccessibleEntityById(runId);
        WorkflowCheckpointEntity checkpoint = checkpointService.latestCheckpoint(runId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.WORKFLOW_CHECKPOINT_INVALID,
                        "No checkpoint found for run: " + runId));
        AgentWorkflowState state = checkpointService.toState(checkpoint);
        AgentWorkflowState.WorkflowError error = state.error();
        return WorkflowCheckpointResponse.from(
                checkpoint,
                isRecoverable(state),
                error == null ? null : error.code(),
                error == null ? null : error.message());
    }

    @Transactional
    public AgentWorkflowState prepareResume(UUID runId) {
        AgentRunEntity run = workflowRunService.getAccessibleEntityById(runId);
        validateRunCanRecover(run);
        AgentWorkflowState state = checkpointService.latestState(runId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.WORKFLOW_CHECKPOINT_INVALID,
                        "No checkpoint found for run: " + runId));
        validateRecoverable(state);
        workflowRunService.prepareForRecovery(
                runId,
                statusForStep(state.currentStepIndex(), state.hasError()),
                state.currentStepIndex(),
                run.getFinalAnswer() != null);
        return clearError(state);
    }

    @Transactional
    public AgentWorkflowState prepareApprovalResume(UUID runId, UUID approvalId) {
        AgentRunEntity run = workflowRunService.getEntityById(runId);
        if (run.getStatus() != WorkflowRunStatus.EXECUTING && run.getStatus() != WorkflowRunStatus.WAITING_APPROVAL) {
            throw new BusinessException(
                    ErrorCode.WORKFLOW_RECOVERY_NOT_ALLOWED,
                    "Workflow run cannot resume approval from status: " + run.getStatus());
        }
        AgentWorkflowState state = checkpointService.latestState(runId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.WORKFLOW_CHECKPOINT_INVALID,
                        "No checkpoint found for run: " + runId));
        Map<String, Object> data = new LinkedHashMap<>(clearError(state).data());
        data.put(AgentWorkflowState.APPROVAL_STATUS, "APPROVED");
        data.put(AgentWorkflowState.APPROVAL_ID, approvalId);
        data.put(AgentWorkflowState.NEXT_NODE, com.agentweave.workflow.graph.WorkflowNodeNames.TOOL_NODE);
        workflowRunService.prepareForRecovery(runId, WorkflowRunStatus.EXECUTING, state.currentStepIndex(), false);
        return new AgentWorkflowState(data);
    }

    public String recoveryStartNode(AgentWorkflowState state) {
        return state.plan() == null
                ? com.agentweave.workflow.graph.WorkflowNodeNames.LOAD_CONTEXT
                : com.agentweave.workflow.graph.WorkflowNodeNames.VALIDATE_PLAN;
    }

    public String approvalResumeStartNode() {
        return com.agentweave.workflow.graph.WorkflowNodeNames.TOOL_NODE;
    }

    protected AgentWorkflowState clearError(AgentWorkflowState state) {
        if (!state.hasError()) {
            return state;
        }
        Map<String, Object> data = new LinkedHashMap<>(state.data());
        data.remove(AgentWorkflowState.ERROR);
        data.remove(AgentWorkflowState.NEXT_NODE);
        return new AgentWorkflowState(data);
    }

    protected void validateRecoverable(AgentWorkflowState state) {
        if (!isRecoverable(state)) {
            AgentWorkflowState.WorkflowError error = state.error();
            String reason = error == null
                    ? "Checkpoint has no recoverable execution state"
                    : "Checkpoint error is not recoverable: " + error.code();
            throw new BusinessException(ErrorCode.WORKFLOW_RECOVERY_NOT_ALLOWED, reason);
        }
    }

    protected boolean isRecoverable(AgentWorkflowState state) {
        AgentWorkflowState.WorkflowError error = state.error();
        return error == null || error.recoverable();
    }

    private void validateRunCanRecover(AgentRunEntity run) {
        if (run.getStatus() == WorkflowRunStatus.SUCCEEDED || run.getStatus() == WorkflowRunStatus.CANCELLED) {
            throw new BusinessException(
                    ErrorCode.WORKFLOW_RECOVERY_NOT_ALLOWED,
                    "Workflow run is terminal and cannot be recovered: " + run.getStatus());
        }
    }

    private WorkflowRunStatus statusForStep(int currentStepIndex, boolean hadError) {
        if (hadError) {
            return currentStepIndex == 0 ? WorkflowRunStatus.PLANNING : WorkflowRunStatus.EXECUTING;
        }
        return currentStepIndex == 0 ? WorkflowRunStatus.PLANNING : WorkflowRunStatus.EXECUTING;
    }
}
