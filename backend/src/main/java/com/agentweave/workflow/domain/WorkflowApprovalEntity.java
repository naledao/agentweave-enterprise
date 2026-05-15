package com.agentweave.workflow.domain;

import com.agentweave.tool.domain.ToolRiskLevel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "agent_workflow_approvals")
public class WorkflowApprovalEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "run_id", nullable = false)
    private AgentRunEntity run;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "step_id", nullable = false)
    private AgentStepEntity step;

    @Column(name = "tool_code", nullable = false, length = 120)
    private String toolCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 40)
    private ToolRiskLevel riskLevel;

    @Column(name = "request_summary", columnDefinition = "TEXT")
    private String requestSummary;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private WorkflowApprovalStatus status = WorkflowApprovalStatus.PENDING;

    @Column(name = "requested_by", nullable = false)
    private UUID requestedBy;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "decision_reason", length = 500)
    private String decisionReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "decided_at")
    private Instant decidedAt;

    protected WorkflowApprovalEntity() {
    }

    public WorkflowApprovalEntity(
            UUID id,
            AgentRunEntity run,
            AgentStepEntity step,
            String toolCode,
            ToolRiskLevel riskLevel,
            String requestSummary,
            UUID requestedBy) {
        this.id = id;
        this.run = run;
        this.step = step;
        this.toolCode = toolCode;
        this.riskLevel = riskLevel;
        this.requestSummary = requestSummary;
        this.requestedBy = requestedBy;
        this.status = WorkflowApprovalStatus.PENDING;
    }

    public void approve(UUID approvedBy, String reason, Instant now) {
        this.status = WorkflowApprovalStatus.APPROVED;
        this.approvedBy = approvedBy;
        this.decisionReason = reason;
        this.decidedAt = now;
    }

    public void reject(UUID approvedBy, String reason, Instant now) {
        this.status = WorkflowApprovalStatus.REJECTED;
        this.approvedBy = approvedBy;
        this.decisionReason = reason;
        this.decidedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public AgentRunEntity getRun() {
        return run;
    }

    public AgentStepEntity getStep() {
        return step;
    }

    public String getToolCode() {
        return toolCode;
    }

    public ToolRiskLevel getRiskLevel() {
        return riskLevel;
    }

    public String getRequestSummary() {
        return requestSummary;
    }

    public WorkflowApprovalStatus getStatus() {
        return status;
    }

    public UUID getRequestedBy() {
        return requestedBy;
    }

    public UUID getApprovedBy() {
        return approvedBy;
    }

    public String getDecisionReason() {
        return decisionReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getDecidedAt() {
        return decidedAt;
    }
}
