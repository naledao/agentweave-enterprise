package com.agentweave.knowledge.domain;

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
@Table(name = "documents")
public class DocumentEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 255)
    private String filename;

    @Column(nullable = false, length = 160)
    private String contentType;

    @Column(nullable = false)
    private long fileSize;

    @Column(nullable = false, length = 120)
    private String storageBucket;

    @Column(nullable = false, length = 500)
    private String storageObjectKey;

    @Column(nullable = false, length = 128)
    private String checksum;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private DocumentStatus status = DocumentStatus.UPLOADED;

    @Column(nullable = false)
    private UUID uploadedBy;

    @Column(nullable = false, length = 160)
    private String source;

    @Column(nullable = false, length = 120)
    private String businessDomain;

    @Column(nullable = false, length = 80)
    private String documentType;

    @Column(nullable = false, length = 80)
    private String permissionLevel;

    private Instant effectiveFrom;

    private Instant effectiveTo;

    @Column(nullable = false)
    private String tags = "";

    @Column(length = 1000)
    private String errorMessage;

    @Column(length = 120)
    private String traceId;

    @Column(name = "cleaned_text", columnDefinition = "TEXT")
    private String cleanedText;

    @Column(name = "text_length")
    private Integer textLength;

    private Instant indexedAt;

    @Column(nullable = false)
    private int reindexCount;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    protected DocumentEntity() {
    }

    public DocumentEntity(
            UUID id,
            String filename,
            String contentType,
            long fileSize,
            String storageBucket,
            String storageObjectKey,
            String checksum,
            UUID uploadedBy,
            String source,
            String businessDomain,
            String documentType,
            String permissionLevel,
            Instant effectiveFrom,
            Instant effectiveTo,
            String tags) {
        this.id = id;
        this.filename = filename;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.storageBucket = storageBucket;
        this.storageObjectKey = storageObjectKey;
        this.checksum = checksum;
        this.status = DocumentStatus.UPLOADED;
        this.uploadedBy = uploadedBy;
        this.source = source;
        this.businessDomain = businessDomain;
        this.documentType = documentType;
        this.permissionLevel = permissionLevel;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
        this.tags = tags == null ? "" : tags;
    }

    public UUID getId() {
        return id;
    }

    public String getFilename() {
        return filename;
    }

    public String getContentType() {
        return contentType;
    }

    public long getFileSize() {
        return fileSize;
    }

    public String getStorageBucket() {
        return storageBucket;
    }

    public String getStorageObjectKey() {
        return storageObjectKey;
    }

    public String getChecksum() {
        return checksum;
    }

    public DocumentStatus getStatus() {
        return status;
    }

    public void markUploaded(String traceId) {
        this.status = DocumentStatus.UPLOADED;
        this.cleanedText = null;
        this.textLength = null;
        this.errorMessage = null;
        this.traceId = traceId;
    }

    public void markParsing(String traceId) {
        this.status = DocumentStatus.PARSING;
        this.cleanedText = null;
        this.textLength = null;
        this.errorMessage = null;
        this.traceId = traceId;
    }

    public void markCleaning(String cleanedText, int textLength, String traceId) {
        this.status = DocumentStatus.CLEANING;
        this.cleanedText = cleanedText;
        this.textLength = textLength;
        this.errorMessage = null;
        this.traceId = traceId;
    }

    public void markChunking(String traceId) {
        this.status = DocumentStatus.CHUNKING;
        this.errorMessage = null;
        this.traceId = traceId;
    }

    public void markEmbedding(String traceId) {
        this.status = DocumentStatus.EMBEDDING;
        this.errorMessage = null;
        this.traceId = traceId;
    }

    public void markIndexed(String traceId) {
        this.status = DocumentStatus.INDEXED;
        this.errorMessage = null;
        this.traceId = traceId;
        this.indexedAt = Instant.now();
    }

    public void markReindexed(String traceId) {
        markIndexed(traceId);
        this.reindexCount++;
    }

    public void markFailed(String errorMessage, String traceId) {
        this.status = DocumentStatus.FAILED;
        this.errorMessage = errorMessage;
        this.traceId = traceId;
    }

    public UUID getUploadedBy() {
        return uploadedBy;
    }

    public String getSource() {
        return source;
    }

    public String getBusinessDomain() {
        return businessDomain;
    }

    public String getDocumentType() {
        return documentType;
    }

    public String getPermissionLevel() {
        return permissionLevel;
    }

    public Instant getEffectiveFrom() {
        return effectiveFrom;
    }

    public Instant getEffectiveTo() {
        return effectiveTo;
    }

    public String getTags() {
        return tags;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getTraceId() {
        return traceId;
    }

    public String getCleanedText() {
        return cleanedText;
    }

    public Integer getTextLength() {
        return textLength;
    }

    public String getParsedText() {
        return cleanedText;
    }

    public Instant getIndexedAt() {
        return indexedAt;
    }

    public int getReindexCount() {
        return reindexCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
