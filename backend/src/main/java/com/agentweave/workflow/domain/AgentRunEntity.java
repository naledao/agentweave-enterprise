package com.agentweave.workflow.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "agent_runs")
public class AgentRunEntity {

    @Id
    private UUID id;

    @Column(name = "conversation_id")
    private UUID conversationId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "trace_id", length = 120)
    private String traceId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String goal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private WorkflowRunStatus status = WorkflowRunStatus.CREATED;

    @Column(name = "current_step_index", nullable = false)
    private int currentStepIndex = 0;

    @Column(name = "final_answer", columnDefinition = "TEXT")
    private String finalAnswer;

    @Column(name = "error_code", length = 80)
    private String errorCode;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "run", fetch = FetchType.LAZY)
    @OrderBy("stepIndex ASC")
    private List<AgentStepEntity> steps = new ArrayList<>();

    protected AgentRunEntity() {
    }

    public AgentRunEntity(UUID id, UUID userId, String goal) {
        this.id = id;
        this.userId = userId;
        this.goal = goal;
        this.status = WorkflowRunStatus.CREATED;
        this.currentStepIndex = 0;
    }

    public void startPlanning(Instant now) {
        this.status = WorkflowRunStatus.PLANNING;
        this.startedAt = now;
    }

    public void startExecuting(Instant now) {
        this.status = WorkflowRunStatus.EXECUTING;
    }

    public void startReviewing(Instant now) {
        this.status = WorkflowRunStatus.REVIEWING;
    }

    public void waitForApproval() {
        this.status = WorkflowRunStatus.WAITING_APPROVAL;
    }

    public void succeed(String finalAnswer, Instant now) {
        this.status = WorkflowRunStatus.SUCCEEDED;
        this.finalAnswer = finalAnswer;
        this.finishedAt = now;
    }

    public void fail(String errorCode, String errorMessage, Instant now) {
        this.status = WorkflowRunStatus.FAILED;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.finishedAt = now;
    }

    public void cancel(Instant now) {
        this.status = WorkflowRunStatus.CANCELLED;
        this.finishedAt = now;
    }

    public void prepareRecovery(WorkflowRunStatus status, int currentStepIndex, boolean clearFinalAnswer, Instant now) {
        this.status = status;
        this.currentStepIndex = currentStepIndex;
        this.errorCode = null;
        this.errorMessage = null;
        this.finishedAt = null;
        if (clearFinalAnswer) {
            this.finalAnswer = null;
        }
        if (this.startedAt == null) {
            this.startedAt = now;
        }
    }

    public void advanceStep() {
        this.currentStepIndex++;
    }

    public void setCurrentStepIndex(int currentStepIndex) {
        this.currentStepIndex = currentStepIndex;
    }

    public UUID getId() {
        return id;
    }

    public UUID getConversationId() {
        return conversationId;
    }

    public void setConversationId(UUID conversationId) {
        this.conversationId = conversationId;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getGoal() {
        return goal;
    }

    public WorkflowRunStatus getStatus() {
        return status;
    }

    public int getCurrentStepIndex() {
        return currentStepIndex;
    }

    public String getFinalAnswer() {
        return finalAnswer;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<AgentStepEntity> getSteps() {
        return steps;
    }
}
