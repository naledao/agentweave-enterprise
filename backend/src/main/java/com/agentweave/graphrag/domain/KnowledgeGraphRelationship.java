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
@Table(name = "kg_relationships")
public class KnowledgeGraphRelationship {

    @Id
    private UUID id;

    @Column(name = "source_document_id", nullable = false)
    private UUID sourceDocumentId;

    @Column(name = "source_entity_id", nullable = false)
    private UUID sourceEntityId;

    @Column(name = "target_entity_id", nullable = false)
    private UUID targetEntityId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private KnowledgeGraphRelationshipType type;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private double confidence;

    @Column(name = "source_chunk_id", nullable = false)
    private UUID sourceChunkId;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    protected KnowledgeGraphRelationship() {
    }

    public KnowledgeGraphRelationship(
            UUID id,
            UUID sourceDocumentId,
            UUID sourceEntityId,
            UUID targetEntityId,
            KnowledgeGraphRelationshipType type,
            String description,
            double confidence,
            UUID sourceChunkId) {
        this.id = id;
        this.sourceDocumentId = sourceDocumentId;
        this.sourceEntityId = sourceEntityId;
        this.targetEntityId = targetEntityId;
        this.type = type;
        this.description = description;
        this.confidence = confidence;
        this.sourceChunkId = sourceChunkId;
    }

    public UUID getId() {
        return id;
    }

    public UUID getSourceDocumentId() {
        return sourceDocumentId;
    }

    public UUID getSourceEntityId() {
        return sourceEntityId;
    }

    public UUID getTargetEntityId() {
        return targetEntityId;
    }

    public KnowledgeGraphRelationshipType getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public double getConfidence() {
        return confidence;
    }

    public UUID getSourceChunkId() {
        return sourceChunkId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
