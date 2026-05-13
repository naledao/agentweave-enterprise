package com.agentweave.knowledge.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "document_chunks")
public class DocumentChunkEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID documentId;

    @Column(nullable = false)
    private int chunkIndex;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private int contentLength;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> metadata = new LinkedHashMap<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private DocumentChunkStatus status = DocumentChunkStatus.PENDING_EMBEDDING;

    @Column(length = 1000)
    private String errorMessage;

    private UUID vectorId;

    private Instant embeddedAt;

    @Column(length = 120)
    private String traceId;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    protected DocumentChunkEntity() {
    }

    public DocumentChunkEntity(
            UUID id,
            UUID documentId,
            int chunkIndex,
            String content,
            Map<String, Object> metadata) {
        this.id = id;
        this.documentId = documentId;
        this.chunkIndex = chunkIndex;
        this.content = content;
        this.contentLength = content.length();
        this.metadata = new LinkedHashMap<>(metadata);
        this.status = DocumentChunkStatus.PENDING_EMBEDDING;
    }

    public UUID getId() {
        return id;
    }

    public UUID getDocumentId() {
        return documentId;
    }

    public int getChunkIndex() {
        return chunkIndex;
    }

    public String getContent() {
        return content;
    }

    public int getContentLength() {
        return contentLength;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public DocumentChunkStatus getStatus() {
        return status;
    }

    public void markEmbedding(String traceId) {
        this.status = DocumentChunkStatus.EMBEDDING;
        this.errorMessage = null;
        this.traceId = traceId;
    }

    public void markIndexed(UUID vectorId, String traceId) {
        this.status = DocumentChunkStatus.INDEXED;
        this.vectorId = vectorId;
        this.embeddedAt = Instant.now();
        this.errorMessage = null;
        this.traceId = traceId;
    }

    public void markFailed(String errorMessage, String traceId) {
        this.status = DocumentChunkStatus.FAILED;
        this.errorMessage = errorMessage;
        this.traceId = traceId;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public UUID getVectorId() {
        return vectorId;
    }

    public Instant getEmbeddedAt() {
        return embeddedAt;
    }

    public String getTraceId() {
        return traceId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
