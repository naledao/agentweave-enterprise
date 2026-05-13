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
@Table(name = "kg_entity_aliases")
public class KnowledgeGraphEntityAlias {

    @Id
    private UUID id;

    @Column(name = "entity_id", nullable = false)
    private UUID entityId;

    @Column(name = "source_document_id", nullable = false)
    private UUID sourceDocumentId;

    @Column(nullable = false, length = 255)
    private String alias;

    @Column(name = "normalized_alias", nullable = false, length = 255)
    private String normalizedAlias;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    protected KnowledgeGraphEntityAlias() {
    }

    public KnowledgeGraphEntityAlias(
            UUID id,
            UUID entityId,
            UUID sourceDocumentId,
            String alias,
            String normalizedAlias) {
        this.id = id;
        this.entityId = entityId;
        this.sourceDocumentId = sourceDocumentId;
        this.alias = alias;
        this.normalizedAlias = normalizedAlias;
    }

    public UUID getId() {
        return id;
    }

    public UUID getEntityId() {
        return entityId;
    }

    public UUID getSourceDocumentId() {
        return sourceDocumentId;
    }

    public String getAlias() {
        return alias;
    }

    public String getNormalizedAlias() {
        return normalizedAlias;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
