package com.agentweave.workflow.api;

import com.agentweave.workflow.application.WorkflowApprovalService;
import com.agentweave.workflow.application.WorkflowExecutionService;
import com.agentweave.workflow.dto.ApprovalDecisionRequest;
import com.agentweave.workflow.dto.WorkflowApprovalResponse;
import com.agentweave.workflow.domain.WorkflowApprovalStatus;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/workflows/approvals")
public class WorkflowApprovalController {

    private final WorkflowApprovalService workflowApprovalService;
    private final WorkflowExecutionService workflowExecutionService;

    public WorkflowApprovalController(
            WorkflowApprovalService workflowApprovalService,
            WorkflowExecutionService workflowExecutionService) {
        this.workflowApprovalService = workflowApprovalService;
        this.workflowExecutionService = workflowExecutionService;
    }

    @GetMapping
    public List<WorkflowApprovalResponse> listApprovals(
            @RequestParam(required = false) WorkflowApprovalStatus status) {
        return workflowApprovalService.list(status);
    }

    @GetMapping("/{approvalId}")
    public WorkflowApprovalResponse getApproval(@PathVariable UUID approvalId) {
        return workflowApprovalService.get(approvalId);
    }

    @PostMapping("/{approvalId}/approve")
    public WorkflowApprovalResponse approve(
            @PathVariable UUID approvalId,
            @Valid @RequestBody(required = false) ApprovalDecisionRequest request) {
        WorkflowApprovalService.ApprovalDecision decision = workflowApprovalService.approve(approvalId, request);
        if (decision.shouldResumeWorkflow()) {
            workflowExecutionService.resumeAfterApproval(decision.approval().runId(), decision.approval().approvalId());
        }
        return decision.approval();
    }

    @PostMapping("/{approvalId}/reject")
    public WorkflowApprovalResponse reject(
            @PathVariable UUID approvalId,
            @Valid @RequestBody(required = false) ApprovalDecisionRequest request) {
        return workflowApprovalService.reject(approvalId, request).approval();
    }
}
