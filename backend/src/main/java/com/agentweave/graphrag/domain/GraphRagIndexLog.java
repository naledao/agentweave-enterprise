package com.agentweave.graphrag.domain;

import com.agentweave.knowledge.domain.DocumentEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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

    @ManyToOne
    @JoinColumn(name = "document_id", insertable = false, updatable = false)
    private DocumentEntity document;

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

    @Column(name = "chunk_entity_count", nullable = false)
    private int chunkEntityCount;

    @Column(name = "neo4j_enabled", nullable = false)
    private boolean neo4jEnabled;

    @Column(name = "duration_ms", nullable = false)
    private long durationMs;

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

    public GraphRagIndexLog(UUID id, UUID documentId, String traceId, int chunkCount, boolean neo4jEnabled) {
        this.id = id;
        this.documentId = documentId;
        this.traceId = traceId;
        this.status = GraphRagIndexStatus.PROCESSING;
        this.chunkCount = chunkCount;
        this.chunkEntityCount = 0;
        this.entityCount = 0;
        this.relationshipCount = 0;
        this.neo4jEnabled = neo4jEnabled;
        this.durationMs = 0;
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

    public int getChunkEntityCount() {
        return chunkEntityCount;
    }

    public boolean isNeo4jEnabled() {
        return neo4jEnabled;
    }

    public long getDurationMs() {
        return durationMs;
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

    public void markCompleted(int entityCount, int relationshipCount, int chunkCount, int chunkEntityCount) {
        this.status = GraphRagIndexStatus.INDEXED;
        this.entityCount = entityCount;
        this.relationshipCount = relationshipCount;
        this.chunkCount = chunkCount;
        this.chunkEntityCount = chunkEntityCount;
        this.errorMessage = null;
        complete();
    }

    public void markFailed(String errorMessage, int entityCount, int relationshipCount, int chunkCount, int chunkEntityCount) {
        this.status = GraphRagIndexStatus.FAILED;
        this.entityCount = entityCount;
        this.relationshipCount = relationshipCount;
        this.chunkCount = chunkCount;
        this.chunkEntityCount = chunkEntityCount;
        this.errorMessage = errorMessage;
        complete();
    }

    private void complete() {
        this.completedAt = Instant.now();
        this.durationMs = Math.max(0, completedAt.toEpochMilli() - startedAt.toEpochMilli());
    }
}
