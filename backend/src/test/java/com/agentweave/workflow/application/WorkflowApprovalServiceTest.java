package com.agentweave.workflow.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agentweave.shared.audit.AuditLogService;
import com.agentweave.shared.exception.AccessDeniedBusinessException;
import com.agentweave.shared.security.CurrentUser;
import com.agentweave.shared.security.CurrentUserService;
import com.agentweave.tool.domain.ToolRiskLevel;
import com.agentweave.tool.log.LogMaskingService;
import com.agentweave.workflow.domain.AgentRunEntity;
import com.agentweave.workflow.domain.AgentStepEntity;
import com.agentweave.workflow.domain.AgentStepType;
import com.agentweave.workflow.domain.WorkflowApprovalEntity;
import com.agentweave.workflow.domain.WorkflowApprovalStatus;
import com.agentweave.workflow.domain.WorkflowRunStatus;
import com.agentweave.workflow.dto.ApprovalDecisionRequest;
import com.agentweave.workflow.dto.WorkflowPlanStep;
import com.agentweave.workflow.repository.WorkflowApprovalRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("WorkflowApprovalService")
class WorkflowApprovalServiceTest {

    @Mock
    private WorkflowApprovalRepository approvalRepository;

    @Mock
    private AgentStepService agentStepService;

    @Mock
    private WorkflowRunService workflowRunService;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private AuditLogService auditLogService;

    private WorkflowApprovalService approvalService;
    private UUID requesterId;
    private AgentRunEntity run;
    private AgentStepEntity step;

    @BeforeEach
    void setUp() {
        approvalService = new WorkflowApprovalService(
                approvalRepository,
                agentStepService,
                workflowRunService,
                currentUserService,
                auditLogService,
                new LogMaskingService());
        requesterId = UUID.randomUUID();
        run = new AgentRunEntity(UUID.randomUUID(), requesterId, "Investigate logs");
        step = new AgentStepEntity(UUID.randomUUID(), run, 1, AgentStepType.HUMAN_APPROVAL, "human_approval_node");
    }

    @Test
    @DisplayName("should create pending approval with masked request summary")
    void shouldCreatePendingApprovalWithMaskedRequestSummary() {
        WorkflowPlanStep planStep = new WorkflowPlanStep(
                UUID.randomUUID(),
                0,
                AgentStepType.TOOL_CALL,
                "search token=sk-demo phone=13812345678",
                List.of("token=sk-demo"),
                List.of("summary"),
                "HIGH",
                "tool:log:search",
                null);
        when(approvalRepository.findFirstByRun_IdAndStep_IdOrderByCreatedAtDesc(run.getId(), step.getId()))
                .thenReturn(Optional.empty());
        when(approvalRepository.save(any(WorkflowApprovalEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WorkflowApprovalEntity approval = approvalService.createPendingApproval(run, step, planStep, ToolRiskLevel.HIGH);

        assertThat(approval.getStatus()).isEqualTo(WorkflowApprovalStatus.PENDING);
        assertThat(approval.getRequestSummary())
                .contains("token=******")
                .contains("138****5678")
                .doesNotContain("sk-demo")
                .doesNotContain("13812345678");
        verify(workflowRunService).transitionTo(run.getId(), WorkflowRunStatus.WAITING_APPROVAL);
        verify(auditLogService).recordWorkflowApprovalCreated(approval.getId(), run.getId(), requesterId, "tool:log:search");
    }

    @Test
    @DisplayName("should reject self approval without explicit self permission")
    void shouldRejectSelfApprovalWithoutExplicitSelfPermission() {
        CurrentUser requester = new CurrentUser(requesterId, "requester", "Requester", Set.of("OPERATOR"),
                Set.of(WorkflowApprovalService.APPROVAL_WRITE_PERMISSION));
        WorkflowApprovalEntity approval = approval();
        when(currentUserService.requireCurrentUser()).thenReturn(requester);
        when(approvalRepository.findById(approval.getId())).thenReturn(Optional.of(approval));

        assertThatThrownBy(() -> approvalService.approve(approval.getId(), new ApprovalDecisionRequest("ok")))
                .isInstanceOf(AccessDeniedBusinessException.class)
                .hasMessageContaining("Requester cannot approve own high-risk tool call");
        verify(auditLogService).recordWorkflowApprovalDenied(
                requester,
                approval.getId(),
                "Requester cannot approve own high-risk tool call");
    }

    @Test
    @DisplayName("should approve pending approval and move workflow back to executing")
    void shouldApprovePendingApprovalAndMoveWorkflowBackToExecuting() {
        UUID approverId = UUID.randomUUID();
        CurrentUser approver = new CurrentUser(approverId, "approver", "Approver", Set.of("OPERATOR"),
                Set.of(WorkflowApprovalService.APPROVAL_WRITE_PERMISSION));
        WorkflowApprovalEntity approval = approval();
        when(currentUserService.requireCurrentUser()).thenReturn(approver);
        when(approvalRepository.findById(approval.getId())).thenReturn(Optional.of(approval));
        when(approvalRepository.save(any(WorkflowApprovalEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WorkflowApprovalService.ApprovalDecision decision =
                approvalService.approve(approval.getId(), new ApprovalDecisionRequest("read only"));

        assertThat(decision.shouldResumeWorkflow()).isTrue();
        assertThat(decision.approval().status()).isEqualTo(WorkflowApprovalStatus.APPROVED);
        verify(agentStepService).completeStep(step.getId(), "read only");
        verify(workflowRunService).transitionTo(run.getId(), WorkflowRunStatus.EXECUTING);
        verify(auditLogService).recordWorkflowApprovalApproved(approver, approval.getId(), "read only");
    }

    @Test
    @DisplayName("should allow requester approval only with explicit self approval permission")
    void shouldAllowRequesterApprovalOnlyWithExplicitSelfApprovalPermission() {
        CurrentUser requester = new CurrentUser(requesterId, "requester", "Requester", Set.of("ADMIN"),
                Set.of(
                        WorkflowApprovalService.APPROVAL_WRITE_PERMISSION,
                        WorkflowApprovalService.APPROVAL_SELF_PERMISSION));
        WorkflowApprovalEntity approval = approval();
        when(currentUserService.requireCurrentUser()).thenReturn(requester);
        when(approvalRepository.findById(approval.getId())).thenReturn(Optional.of(approval));
        when(approvalRepository.save(any(WorkflowApprovalEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WorkflowApprovalService.ApprovalDecision decision =
                approvalService.approve(approval.getId(), new ApprovalDecisionRequest("explicit self approval"));

        assertThat(decision.approval().status()).isEqualTo(WorkflowApprovalStatus.APPROVED);
        verify(auditLogService).recordWorkflowApprovalApproved(
                requester,
                approval.getId(),
                "explicit self approval");
    }


    @Test
    @DisplayName("should reject pending approval and fail workflow")
    void shouldRejectPendingApprovalAndFailWorkflow() {
        UUID approverId = UUID.randomUUID();
        CurrentUser approver = new CurrentUser(approverId, "approver", "Approver", Set.of("OPERATOR"),
                Set.of(WorkflowApprovalService.APPROVAL_WRITE_PERMISSION));
        WorkflowApprovalEntity approval = approval();
        when(currentUserService.requireCurrentUser()).thenReturn(approver);
        when(approvalRepository.findById(approval.getId())).thenReturn(Optional.of(approval));
        when(approvalRepository.save(any(WorkflowApprovalEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WorkflowApprovalService.ApprovalDecision decision =
                approvalService.reject(approval.getId(), new ApprovalDecisionRequest("too risky"));

        assertThat(decision.shouldResumeWorkflow()).isFalse();
        assertThat(decision.approval().status()).isEqualTo(WorkflowApprovalStatus.REJECTED);
        verify(agentStepService).failStep(step.getId(), "WORKFLOW_APPROVAL_REJECTED", "too risky");
        verify(workflowRunService).markFailed(run.getId(), "WORKFLOW_APPROVAL_REJECTED",
                "High-risk tool call was rejected");
        verify(auditLogService).recordWorkflowApprovalRejected(approver, approval.getId(), "too risky");
    }

    private WorkflowApprovalEntity approval() {
        return new WorkflowApprovalEntity(
                UUID.randomUUID(),
                run,
                step,
                "tool:log:search",
                ToolRiskLevel.HIGH,
                "masked summary",
                requesterId);
    }
}
