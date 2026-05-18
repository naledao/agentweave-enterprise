package com.agentweave.workflow.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import com.agentweave.workflow.dto.WorkflowReviewResult;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "agent_steps")
public class AgentStepEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "run_id", nullable = false)
    private AgentRunEntity run;

    @Column(name = "step_index", nullable = false)
    private int stepIndex;

    @Enumerated(EnumType.STRING)
    @Column(name = "step_type", nullable = false, length = 40)
    private AgentStepType stepType;

    @Column(name = "node_name", length = 80)
    private String nodeName;

    @Enumerated(EnumType.STRING)
    @Column(name = "agent_role", length = 40)
    private AgentRole agentRole;

    @Column(name = "trace_id", length = 120)
    private String traceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AgentStepStatus status = AgentStepStatus.PENDING;

    @Column(name = "input_summary", columnDefinition = "TEXT")
    private String inputSummary;

    @Column(name = "output_summary", columnDefinition = "TEXT")
    private String outputSummary;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private List<WorkflowReviewResult.Citation> citations = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "graph_paths", nullable = false, columnDefinition = "jsonb")
    private List<WorkflowReviewResult.GraphPath> graphPaths = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tool_calls", nullable = false, columnDefinition = "jsonb")
    private List<WorkflowReviewResult.ToolCallResult> toolCalls = new ArrayList<>();

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    @Column(name = "retry_reason", length = 500)
    private String retryReason;

    @Column(name = "last_retried_at")
    private Instant lastRetriedAt;

    @Column(name = "error_code", length = 80)
    private String errorCode;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AgentStepEntity() {
    }

    public AgentStepEntity(UUID id, AgentRunEntity run, int stepIndex, AgentStepType stepType, String nodeName) {
        this.id = id;
        this.run = run;
        this.stepIndex = stepIndex;
        this.stepType = stepType;
        this.nodeName = nodeName;
        this.agentRole = defaultAgentRole(stepType);
        this.traceId = run == null ? null : run.getTraceId();
        this.status = AgentStepStatus.PENDING;
    }

    public void start(Instant now) {
        this.status = AgentStepStatus.RUNNING;
        this.startedAt = now;
    }

    public void succeed(String outputSummary, Instant now) {
        this.status = AgentStepStatus.SUCCEEDED;
        this.outputSummary = outputSummary;
        this.finishedAt = now;
        calculateDuration();
    }

    public void recordArtifacts(
            List<WorkflowReviewResult.Citation> citations,
            List<WorkflowReviewResult.GraphPath> graphPaths,
            List<WorkflowReviewResult.ToolCallResult> toolCalls) {
        this.citations = citations == null ? new ArrayList<>() : new ArrayList<>(citations);
        this.graphPaths = graphPaths == null ? new ArrayList<>() : new ArrayList<>(graphPaths);
        this.toolCalls = toolCalls == null ? new ArrayList<>() : new ArrayList<>(toolCalls);
    }

    public void fail(String errorCode, String errorMessage, Instant now) {
        this.status = AgentStepStatus.FAILED;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.finishedAt = now;
        calculateDuration();
    }

    public void skip(Instant now) {
        this.status = AgentStepStatus.SKIPPED;
        this.finishedAt = now;
        calculateDuration();
    }

    public void waitForApproval(Instant now) {
        this.status = AgentStepStatus.WAITING_APPROVAL;
        this.startedAt = this.startedAt == null ? now : this.startedAt;
    }

    public void recordRetry(String reason, Instant now) {
        this.retryCount++;
        this.retryReason = reason;
        this.lastRetriedAt = now;
        this.status = AgentStepStatus.RETRYING;
    }

    private void calculateDuration() {
        if (startedAt != null && finishedAt != null) {
            this.durationMs = finishedAt.toEpochMilli() - startedAt.toEpochMilli();
        }
    }

    public UUID getId() {
        return id;
    }

    public AgentRunEntity getRun() {
        return run;
    }

    public int getStepIndex() {
        return stepIndex;
    }

    public AgentStepType getStepType() {
        return stepType;
    }

    public String getNodeName() {
        return nodeName;
    }

    public AgentRole getAgentRole() {
        return agentRole;
    }

    public String getTraceId() {
        return traceId;
    }

    public AgentStepStatus getStatus() {
        return status;
    }

    public String getInputSummary() {
        return inputSummary;
    }

    public void setInputSummary(String inputSummary) {
        this.inputSummary = inputSummary;
    }

    public String getOutputSummary() {
        return outputSummary;
    }

    public List<WorkflowReviewResult.Citation> getCitations() {
        return citations;
    }

    public List<WorkflowReviewResult.GraphPath> getGraphPaths() {
        return graphPaths;
    }

    public List<WorkflowReviewResult.ToolCallResult> getToolCalls() {
        return toolCalls;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public String getRetryReason() {
        return retryReason;
    }

    public Instant getLastRetriedAt() {
        return lastRetriedAt;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    private AgentRole defaultAgentRole(AgentStepType stepType) {
        if (stepType == null) {
            return AgentRole.SYSTEM;
        }
        return switch (stepType) {
            case PLANNING -> AgentRole.PLANNER;
            case REVIEW, FINAL_ANSWER -> AgentRole.REVIEWER;
            case HUMAN_APPROVAL -> AgentRole.APPROVAL;
            case CHECKPOINT, ERROR -> AgentRole.SYSTEM;
            default -> AgentRole.EXECUTOR;
        };
    }
}
