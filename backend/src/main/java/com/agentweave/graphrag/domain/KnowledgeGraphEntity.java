package com.agentweave.graphrag.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "kg_entities")
public class KnowledgeGraphEntity {

    @Id
    private UUID id;

    @Column(name = "source_document_id", nullable = false)
    private UUID sourceDocumentId;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "normalized_name", nullable = false, length = 255)
    private String normalizedName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private KnowledgeGraphEntityType type;

    @Column(columnDefinition = "TEXT")
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private List<String> aliases = new ArrayList<>();

    @Column(name = "business_domain", nullable = false, length = 120)
    private String businessDomain;

    @Column(name = "permission_level", nullable = false, length = 80)
    private String permissionLevel;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "source_chunk_ids", nullable = false, columnDefinition = "jsonb")
    private List<UUID> sourceChunkIds = new ArrayList<>();

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    protected KnowledgeGraphEntity() {
    }

    public KnowledgeGraphEntity(
            UUID id,
            UUID sourceDocumentId,
            String name,
            String normalizedName,
            KnowledgeGraphEntityType type,
            String description,
            List<String> aliases,
            String businessDomain,
            String permissionLevel,
            List<UUID> sourceChunkIds) {
        this.id = id;
        this.sourceDocumentId = sourceDocumentId;
        this.name = name;
        this.normalizedName = normalizedName;
        this.type = type;
        this.description = description;
        this.aliases = aliases == null ? new ArrayList<>() : new ArrayList<>(aliases);
        this.businessDomain = businessDomain;
        this.permissionLevel = permissionLevel;
        this.sourceChunkIds = sourceChunkIds == null ? new ArrayList<>() : new ArrayList<>(sourceChunkIds);
    }

    public UUID getId() {
        return id;
    }

    public UUID getSourceDocumentId() {
        return sourceDocumentId;
    }

    public String getName() {
        return name;
    }

    public String getNormalizedName() {
        return normalizedName;
    }

    public KnowledgeGraphEntityType getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public List<String> getAliases() {
        return List.copyOf(aliases);
    }

    public String getBusinessDomain() {
        return businessDomain;
    }

    public String getPermissionLevel() {
        return permissionLevel;
    }

    public List<UUID> getSourceChunkIds() {
        return List.copyOf(sourceChunkIds);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
