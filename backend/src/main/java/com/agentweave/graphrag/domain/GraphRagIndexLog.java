package com.agentweave.graphrag.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "graphrag_index_logs")
public class GraphRagIndexLog {

    @Id
    private UUID id;

    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @Column(name = "trace_id", nullable = false, length = 120)
    private String traceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private GraphRagIndexStatus status;

    @Column(name = "entity_count", nullable = false)
    private int entityCount;

    @Column(name = "relationship_count", nullable = false)
    private int relationshipCount;

    @Column(name = "chunk_count", nullable = false)
    private int chunkCount;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    protected GraphRagIndexLog() {
    }

    public GraphRagIndexLog(UUID id, UUID documentId, String traceId, int chunkCount) {
        this.id = id;
        this.documentId = documentId;
        this.traceId = traceId;
        this.status = GraphRagIndexStatus.PROCESSING;
        this.chunkCount = chunkCount;
        this.entityCount = 0;
        this.relationshipCount = 0;
        this.startedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getDocumentId() {
        return documentId;
    }

    public String getTraceId() {
        return traceId;
    }

    public GraphRagIndexStatus getStatus() {
        return status;
    }

    public int getEntityCount() {
        return entityCount;
    }

    public int getRelationshipCount() {
        return relationshipCount;
    }

    public int getChunkCount() {
        return chunkCount;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void markCompleted(int entityCount, int relationshipCount, int chunkCount) {
        this.status = GraphRagIndexStatus.INDEXED;
        this.entityCount = entityCount;
        this.relationshipCount = relationshipCount;
        this.chunkCount = chunkCount;
        this.errorMessage = null;
        this.completedAt = Instant.now();
    }

    public void markFailed(String errorMessage, int entityCount, int relationshipCount, int chunkCount) {
        this.status = GraphRagIndexStatus.FAILED;
        this.entityCount = entityCount;
        this.relationshipCount = relationshipCount;
        this.chunkCount = chunkCount;
        this.errorMessage = errorMessage;
        this.completedAt = Instant.now();
    }
}
