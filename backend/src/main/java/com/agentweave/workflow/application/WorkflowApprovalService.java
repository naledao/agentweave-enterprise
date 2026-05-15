package com.agentweave.workflow.application;

import com.agentweave.shared.audit.AuditLogService;
import com.agentweave.shared.exception.AccessDeniedBusinessException;
import com.agentweave.shared.exception.BusinessException;
import com.agentweave.shared.exception.ErrorCode;
import com.agentweave.shared.exception.ResourceNotFoundException;
import com.agentweave.shared.security.CurrentUser;
import com.agentweave.shared.security.CurrentUserService;
import com.agentweave.tool.domain.ToolRiskLevel;
import com.agentweave.tool.log.LogMaskingService;
import com.agentweave.workflow.domain.AgentRunEntity;
import com.agentweave.workflow.domain.AgentStepEntity;
import com.agentweave.workflow.domain.WorkflowApprovalEntity;
import com.agentweave.workflow.domain.WorkflowApprovalStatus;
import com.agentweave.workflow.domain.WorkflowRunStatus;
import com.agentweave.workflow.dto.ApprovalDecisionRequest;
import com.agentweave.workflow.dto.WorkflowApprovalResponse;
import com.agentweave.workflow.dto.WorkflowPlanStep;
import com.agentweave.workflow.repository.WorkflowApprovalRepository;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkflowApprovalService {

    public static final String APPROVAL_READ_PERMISSION = "workflow:approval:read";
    public static final String APPROVAL_WRITE_PERMISSION = "workflow:approval:write";
    public static final String APPROVAL_SELF_PERMISSION = "workflow:approval:self";

    private final WorkflowApprovalRepository approvalRepository;
    private final AgentStepService agentStepService;
    private final WorkflowRunService workflowRunService;
    private final CurrentUserService currentUserService;
    private final AuditLogService auditLogService;
    private final LogMaskingService maskingService;

    public WorkflowApprovalService(
            WorkflowApprovalRepository approvalRepository,
            AgentStepService agentStepService,
            WorkflowRunService workflowRunService,
            CurrentUserService currentUserService,
            AuditLogService auditLogService,
            LogMaskingService maskingService) {
        this.approvalRepository = approvalRepository;
        this.agentStepService = agentStepService;
        this.workflowRunService = workflowRunService;
        this.currentUserService = currentUserService;
        this.auditLogService = auditLogService;
        this.maskingService = maskingService;
    }

    @Transactional
    public WorkflowApprovalEntity createPendingApproval(
            AgentRunEntity run,
            AgentStepEntity stepEntity,
            WorkflowPlanStep planStep,
            ToolRiskLevel riskLevel) {
        return approvalRepository.findFirstByRun_IdAndStep_IdOrderByCreatedAtDesc(run.getId(), stepEntity.getId())
                .orElseGet(() -> createApproval(run, stepEntity, planStep, riskLevel));
    }

    @Transactional(readOnly = true)
    public List<WorkflowApprovalResponse> list(WorkflowApprovalStatus status) {
        CurrentUser user = currentUserService.requireCurrentUser();
        List<WorkflowApprovalEntity> approvals;
        if (user.hasRole("ADMIN") || user.hasPermission(APPROVAL_READ_PERMISSION)) {
            approvals = status == null
                    ? approvalRepository.findAllByOrderByCreatedAtDesc()
                    : approvalRepository.findByStatusOrderByCreatedAtDesc(status);
        } else {
            approvals = status == null
                    ? approvalRepository.findByRequestedByOrderByCreatedAtDesc(user.id())
                    : approvalRepository.findByRequestedByAndStatusOrderByCreatedAtDesc(user.id(), status);
        }
        return approvals.stream()
                .map(WorkflowApprovalResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public WorkflowApprovalResponse get(UUID approvalId) {
        CurrentUser user = currentUserService.requireCurrentUser();
        WorkflowApprovalEntity approval = require(approvalId);
        validateReadAccess(user, approval);
        return WorkflowApprovalResponse.from(approval);
    }

    @Transactional
    public ApprovalDecision approve(UUID approvalId, ApprovalDecisionRequest request) {
        CurrentUser user = currentUserService.requireCurrentUser();
        WorkflowApprovalEntity approval = require(approvalId);
        validateDecisionAccess(user, approval);
        validatePending(approval);

        String reason = sanitizedReason(request);
        approval.approve(user.id(), reason, Instant.now());
        WorkflowApprovalEntity saved = approvalRepository.save(approval);
        agentStepService.completeStep(saved.getStep().getId(),
                reason == null || reason.isBlank() ? "High-risk tool call approved" : reason);
        workflowRunService.transitionTo(saved.getRun().getId(), WorkflowRunStatus.EXECUTING);
        auditLogService.recordWorkflowApprovalApproved(user, saved.getId(), reason);
        return new ApprovalDecision(WorkflowApprovalResponse.from(saved), true);
    }

    @Transactional
    public ApprovalDecision reject(UUID approvalId, ApprovalDecisionRequest request) {
        CurrentUser user = currentUserService.requireCurrentUser();
        WorkflowApprovalEntity approval = require(approvalId);
        validateDecisionAccess(user, approval);
        validatePending(approval);

        String reason = sanitizedReason(request);
        approval.reject(user.id(), reason, Instant.now());
        WorkflowApprovalEntity saved = approvalRepository.save(approval);
        agentStepService.failStep(saved.getStep().getId(), "WORKFLOW_APPROVAL_REJECTED",
                reason == null || reason.isBlank() ? "High-risk tool call was rejected" : reason);
        workflowRunService.markFailed(saved.getRun().getId(), "WORKFLOW_APPROVAL_REJECTED",
                "High-risk tool call was rejected");
        auditLogService.recordWorkflowApprovalRejected(user, saved.getId(), reason);
        return new ApprovalDecision(WorkflowApprovalResponse.from(saved), false);
    }

    private WorkflowApprovalEntity createApproval(
            AgentRunEntity run,
            AgentStepEntity stepEntity,
            WorkflowPlanStep planStep,
            ToolRiskLevel riskLevel) {
        WorkflowApprovalEntity approval = new WorkflowApprovalEntity(
                UUID.randomUUID(),
                run,
                stepEntity,
                normalizeToolCode(planStep.toolCode()),
                riskLevel,
                requestSummary(planStep),
                run.getUserId());
        WorkflowApprovalEntity saved = approvalRepository.save(approval);
        workflowRunService.transitionTo(run.getId(), WorkflowRunStatus.WAITING_APPROVAL);
        auditLogService.recordWorkflowApprovalCreated(saved.getId(), run.getId(), run.getUserId(), saved.getToolCode());
        return saved;
    }

    private WorkflowApprovalEntity require(UUID approvalId) {
        return approvalRepository.findById(approvalId)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow approval not found: " + approvalId));
    }

    private void validateReadAccess(CurrentUser user, WorkflowApprovalEntity approval) {
        if (user.hasRole("ADMIN")
                || user.hasPermission(APPROVAL_READ_PERMISSION)
                || user.id().equals(approval.getRequestedBy())) {
            return;
        }
        throw new ResourceNotFoundException("Workflow approval not found: " + approval.getId());
    }

    private void validateDecisionAccess(CurrentUser user, WorkflowApprovalEntity approval) {
        boolean canDecide = user.hasRole("ADMIN") || user.hasPermission(APPROVAL_WRITE_PERMISSION);
        if (!canDecide) {
            auditLogService.recordWorkflowApprovalDenied(user, approval.getId(), "Missing workflow approval permission");
            throw new AccessDeniedBusinessException("Missing workflow approval permission");
        }
        if (user.id().equals(approval.getRequestedBy())
                && !user.hasPermission(APPROVAL_SELF_PERMISSION)) {
            auditLogService.recordWorkflowApprovalDenied(user, approval.getId(), "Requester cannot approve own high-risk tool call");
            throw new AccessDeniedBusinessException("Requester cannot approve own high-risk tool call");
        }
    }

    private void validatePending(WorkflowApprovalEntity approval) {
        if (approval.getStatus() != WorkflowApprovalStatus.PENDING) {
            throw new BusinessException(
                    ErrorCode.WORKFLOW_RECOVERY_NOT_ALLOWED,
                    "Workflow approval has already been decided: " + approval.getStatus());
        }
    }

    private String requestSummary(WorkflowPlanStep step) {
        String summary = "tool=" + normalizeToolCode(step.toolCode())
                + "; instruction=" + nullToEmpty(step.instruction())
                + "; requiredInputs=" + step.requiredInputs()
                + "; expectedOutputs=" + step.expectedOutputs();
        return truncate(maskingService.mask(summary), 1000);
    }

    private String sanitizedReason(ApprovalDecisionRequest request) {
        if (request == null || request.reason() == null || request.reason().isBlank()) {
            return null;
        }
        return truncate(maskingService.mask(request.reason().trim()), 500);
    }

    private String normalizeToolCode(String toolCode) {
        return toolCode == null ? "" : toolCode.trim().toLowerCase(Locale.ROOT);
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    public record ApprovalDecision(
            WorkflowApprovalResponse approval,
            boolean shouldResumeWorkflow
    ) {
    }
}
