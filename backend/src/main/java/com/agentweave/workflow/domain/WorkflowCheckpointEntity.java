package com.agentweave.workflow.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "agent_workflow_checkpoints")
public class WorkflowCheckpointEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "run_id", nullable = false)
    private AgentRunEntity run;

    @Column(name = "step_index", nullable = false)
    private int stepIndex;

    @Column(name = "node_name", nullable = false, length = 80)
    private String nodeName;

    @Column(name = "state_version", nullable = false)
    private int stateVersion;

    @Column(name = "state_payload", nullable = false, columnDefinition = "TEXT")
    private String statePayload;

    @Column(nullable = false, length = 128)
    private String checksum;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected WorkflowCheckpointEntity() {
    }

    public WorkflowCheckpointEntity(
            UUID id,
            AgentRunEntity run,
            int stepIndex,
            String nodeName,
            int stateVersion,
            String statePayload,
            String checksum) {
        this.id = id;
        this.run = run;
        this.stepIndex = stepIndex;
        this.nodeName = nodeName;
        this.stateVersion = stateVersion;
        this.statePayload = statePayload;
        this.checksum = checksum;
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

    public String getNodeName() {
        return nodeName;
    }

    public int getStateVersion() {
        return stateVersion;
    }

    public String getStatePayload() {
        return statePayload;
    }

    public String getChecksum() {
        return checksum;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
