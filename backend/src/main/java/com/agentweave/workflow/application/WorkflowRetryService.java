package com.agentweave.workflow.application;

import com.agentweave.shared.exception.BusinessException;
import com.agentweave.shared.exception.ErrorCode;
import com.agentweave.workflow.dto.WorkflowRetryRequest;
import com.agentweave.workflow.state.AgentWorkflowState;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkflowRetryService {

    private final WorkflowRecoveryService recoveryService;
    private final WorkflowRunService workflowRunService;
    private final AgentStepService agentStepService;

    public WorkflowRetryService(
            WorkflowRecoveryService recoveryService,
            WorkflowRunService workflowRunService,
            AgentStepService agentStepService) {
        this.recoveryService = recoveryService;
        this.workflowRunService = workflowRunService;
        this.agentStepService = agentStepService;
    }

    @Transactional
    public AgentWorkflowState prepareRetry(UUID runId, WorkflowRetryRequest request) {
        AgentWorkflowState state = recoveryService.prepareResume(runId);
        int retryStepIndex = resolveRetryStepIndex(state, request);
        Map<String, Object> data = new LinkedHashMap<>(state.data());
        data.put(AgentWorkflowState.CURRENT_STEP_INDEX, retryStepIndex);
        data.put(AgentWorkflowState.STEP_RESULTS, truncateList(state.stepResults(), retryStepIndex));
        data.put(AgentWorkflowState.CITATIONS, List.of());
        data.put(AgentWorkflowState.GRAPH_PATHS, List.of());
        data.put(AgentWorkflowState.TOOL_CALLS, List.of());
        data.remove(AgentWorkflowState.APPROVAL_STATUS);
        data.remove(AgentWorkflowState.FINAL_ANSWER);
        data.remove(AgentWorkflowState.ERROR);
        data.remove(AgentWorkflowState.NEXT_NODE);

        workflowRunService.prepareForRecovery(runId, statusForRetryStep(retryStepIndex), retryStepIndex, true);
        agentStepService.recordRetry(runId, persistedStepIndex(state, retryStepIndex), normalizeReason(request));
        return new AgentWorkflowState(data);
    }

    private int resolveRetryStepIndex(AgentWorkflowState state, WorkflowRetryRequest request) {
        int retryStepIndex = request == null || request.fromStepIndex() == null
                ? defaultRetryStepIndex(state)
                : request.fromStepIndex();
        if (state.plan() == null) {
            if (retryStepIndex != 0) {
                throw new BusinessException(
                        ErrorCode.WORKFLOW_RECOVERY_NOT_ALLOWED,
                        "Cannot retry from step " + retryStepIndex + " before a workflow plan exists");
            }
            return retryStepIndex;
        }
        int maxStepIndex = state.plan().steps().size();
        if (retryStepIndex < 0 || retryStepIndex > maxStepIndex) {
            throw new BusinessException(
                    ErrorCode.WORKFLOW_RECOVERY_NOT_ALLOWED,
                    "Retry step index out of range: " + retryStepIndex);
        }
        return retryStepIndex;
    }

    private int defaultRetryStepIndex(AgentWorkflowState state) {
        AgentWorkflowState.WorkflowError error = state.error();
        if (error != null && error.stepIndex() != null) {
            return error.stepIndex();
        }
        return state.currentStepIndex();
    }

    private List<?> truncateList(List<?> values, int size) {
        if (values == null || values.isEmpty() || size <= 0) {
            return List.of();
        }
        return new ArrayList<>(values.subList(0, Math.min(size, values.size())));
    }

    private com.agentweave.workflow.domain.WorkflowRunStatus statusForRetryStep(int retryStepIndex) {
        return retryStepIndex == 0
                ? com.agentweave.workflow.domain.WorkflowRunStatus.PLANNING
                : com.agentweave.workflow.domain.WorkflowRunStatus.EXECUTING;
    }

    private int persistedStepIndex(AgentWorkflowState state, int planStepIndex) {
        if (state.plan() == null && planStepIndex == 0) {
            return 0;
        }
        return planStepIndex + 1;
    }

    private String normalizeReason(WorkflowRetryRequest request) {
        if (request == null || request.reason() == null || request.reason().isBlank()) {
            return "Manual workflow retry";
        }
        return request.reason();
    }
}
