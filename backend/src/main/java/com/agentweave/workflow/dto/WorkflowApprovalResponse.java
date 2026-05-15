package com.agentweave.workflow.dto;

import com.agentweave.tool.domain.ToolRiskLevel;
import com.agentweave.workflow.domain.WorkflowApprovalEntity;
import com.agentweave.workflow.domain.WorkflowApprovalStatus;
import java.time.Instant;
import java.util.UUID;

public record WorkflowApprovalResponse(
        UUID approvalId,
        UUID runId,
        UUID stepId,
        int stepIndex,
        String toolCode,
        ToolRiskLevel riskLevel,
        String requestSummary,
        WorkflowApprovalStatus status,
        UUID requestedBy,
        UUID approvedBy,
        String decisionReason,
        Instant createdAt,
        Instant decidedAt
) {

    public static WorkflowApprovalResponse from(WorkflowApprovalEntity entity) {
        return new WorkflowApprovalResponse(
                entity.getId(),
                entity.getRun().getId(),
                entity.getStep().getId(),
                entity.getStep().getStepIndex(),
                entity.getToolCode(),
                entity.getRiskLevel(),
                entity.getRequestSummary(),
                entity.getStatus(),
                entity.getRequestedBy(),
                entity.getApprovedBy(),
                entity.getDecisionReason(),
                entity.getCreatedAt(),
                entity.getDecidedAt());
    }
}
