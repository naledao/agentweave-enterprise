package com.agentweave.workflow.api;

import com.agentweave.workflow.application.WorkflowExecutionService;
import com.agentweave.workflow.application.WorkflowRecoveryService;
import com.agentweave.workflow.application.WorkflowRunService;
import com.agentweave.workflow.application.PlanValidator;
import com.agentweave.langchain4j.agent.PlannerAgent;
import com.agentweave.workflow.dto.WorkflowPlan;
import com.agentweave.workflow.application.PlanValidator.ValidationResult;
import com.agentweave.workflow.dto.CreateWorkflowRunRequest;
import com.agentweave.workflow.dto.WorkflowCheckpointResponse;
import com.agentweave.workflow.dto.WorkflowRetryRequest;
import com.agentweave.workflow.dto.WorkflowRunListResponse;
import com.agentweave.workflow.dto.WorkflowRunQueryRequest;
import com.agentweave.workflow.dto.WorkflowRunResponse;
import com.agentweave.workflow.dto.WorkflowStepResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/workflows")
public class WorkflowController {

    private final WorkflowRunService workflowRunService;
    private final WorkflowExecutionService workflowExecutionService;
    private final WorkflowRecoveryService workflowRecoveryService;
    private final PlannerAgent plannerAgent;
    private final PlanValidator planValidator;

    public WorkflowController(WorkflowRunService workflowRunService,
                             WorkflowExecutionService workflowExecutionService,
                             WorkflowRecoveryService workflowRecoveryService,
                             PlannerAgent plannerAgent,
                             PlanValidator planValidator) {
        this.workflowRunService = workflowRunService;
        this.workflowExecutionService = workflowExecutionService;
        this.workflowRecoveryService = workflowRecoveryService;
        this.plannerAgent = plannerAgent;
        this.planValidator = planValidator;
    }

    @GetMapping("/runs")
    public WorkflowRunListResponse listRuns(@Valid WorkflowRunQueryRequest request) {
        return workflowRunService.list(request);
    }

    @PostMapping("/runs")
    @ResponseStatus(HttpStatus.CREATED)
    public WorkflowRunResponse createRun(@Valid @RequestBody CreateWorkflowRunRequest request) {
        WorkflowRunResponse runResponse = workflowRunService.create(request);
        return workflowExecutionService.executeWorkflow(runResponse.runId());
    }

    @GetMapping("/runs/{runId}")
    public WorkflowRunResponse getRun(@PathVariable UUID runId) {
        return workflowRunService.get(runId);
    }

    @GetMapping("/runs/{runId}/steps")
    public List<WorkflowStepResponse> getSteps(@PathVariable UUID runId) {
        return workflowRunService.getSteps(runId);
    }

    @PostMapping("/runs/{runId}/cancel")
    public WorkflowRunResponse cancelRun(@PathVariable UUID runId) {
        return workflowRunService.cancel(runId);
    }

    @PostMapping("/runs/{runId}/resume")
    public WorkflowRunResponse resumeRun(@PathVariable UUID runId) {
        return workflowExecutionService.resumeWorkflow(runId);
    }

    @GetMapping("/runs/{runId}/checkpoints/latest")
    public WorkflowCheckpointResponse getLatestCheckpoint(@PathVariable UUID runId) {
        return workflowRecoveryService.latestCheckpoint(runId);
    }

    @PostMapping("/runs/{runId}/retry")
    public WorkflowRunResponse retryRun(
            @PathVariable UUID runId,
            @Valid @RequestBody(required = false) WorkflowRetryRequest request) {
        return workflowExecutionService.retryWorkflow(runId, request);
    }

    @PostMapping("/internal/plans/preview")
    public PlanPreviewResponse previewPlan(@Valid @RequestBody PlanPreviewRequest request) {
        WorkflowPlan plan = plannerAgent.createPlan(request.goal(), null);
        ValidationResult validation = planValidator.validate(plan);
        return new PlanPreviewResponse(plan, validation.passed(), validation.message());
    }

    public record PlanPreviewRequest(String goal) {}

    public record PlanPreviewResponse(WorkflowPlan plan, boolean valid, String validationMessage) {}
}
