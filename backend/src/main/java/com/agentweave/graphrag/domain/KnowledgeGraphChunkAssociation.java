package com.agentweave.graphrag.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "kg_chunk_entities")
public class KnowledgeGraphChunkAssociation {

    @Id
    private UUID id;

    @Column(name = "source_document_id", nullable = false)
    private UUID sourceDocumentId;

    @Column(name = "chunk_id", nullable = false)
    private UUID chunkId;

    @Column(name = "entity_id", nullable = false)
    private UUID entityId;

    @Column(name = "mention_count", nullable = false)
    private int mentionCount;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    protected KnowledgeGraphChunkAssociation() {
    }

    public KnowledgeGraphChunkAssociation(
            UUID id,
            UUID sourceDocumentId,
            UUID chunkId,
            UUID entityId,
            int mentionCount) {
        this.id = id;
        this.sourceDocumentId = sourceDocumentId;
        this.chunkId = chunkId;
        this.entityId = entityId;
        this.mentionCount = mentionCount;
    }

    public UUID getId() {
        return id;
    }

    public UUID getSourceDocumentId() {
        return sourceDocumentId;
    }

    public UUID getChunkId() {
        return chunkId;
    }

    public UUID getEntityId() {
        return entityId;
    }

    public int getMentionCount() {
        return mentionCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
