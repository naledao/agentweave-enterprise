package com.agentweave.workflow.api;

import com.agentweave.workflow.application.WorkflowRunService;
import com.agentweave.workflow.dto.CreateWorkflowRunRequest;
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

    public WorkflowController(WorkflowRunService workflowRunService) {
        this.workflowRunService = workflowRunService;
    }

    @PostMapping("/runs")
    @ResponseStatus(HttpStatus.CREATED)
    public WorkflowRunResponse createRun(@Valid @RequestBody CreateWorkflowRunRequest request) {
        return workflowRunService.create(request);
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
}
