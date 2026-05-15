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
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

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
    @Column(nullable = false, length = 40)
    private AgentStepStatus status = AgentStepStatus.PENDING;

    @Column(name = "input_summary", columnDefinition = "TEXT")
    private String inputSummary;

    @Column(name = "output_summary", columnDefinition = "TEXT")
    private String outputSummary;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "duration_ms")
    private Long durationMs;

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

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public Long getDurationMs() {
        return durationMs;
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
}
