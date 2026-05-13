package com.agentweave.knowledge.dto;

import com.agentweave.knowledge.domain.DocumentEntity;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public record DocumentResponse(
        UUID documentId,
        String filename,
        String contentType,
        long fileSize,
        UUID uploadedBy,
        String status,
        String errorMessage,
        String traceId,
        long chunkCount,
        int reindexCount,
        Instant indexedAt,
        StorageSummary storage,
        DocumentMetadataResponse metadata,
        Instant createdAt,
        Instant updatedAt) {

    public static DocumentResponse from(DocumentEntity document, long chunkCount) {
        return new DocumentResponse(
                document.getId(),
                document.getFilename(),
                document.getContentType(),
                document.getFileSize(),
                document.getUploadedBy(),
                document.getStatus().name().toLowerCase(Locale.ROOT),
                document.getErrorMessage(),
                document.getTraceId(),
                chunkCount,
                document.getReindexCount(),
                document.getIndexedAt(),
                new StorageSummary(
                        document.getStorageBucket(),
                        document.getStorageObjectKey(),
                        document.getChecksum()),
                new DocumentMetadataResponse(
                        document.getSource(),
                        document.getBusinessDomain(),
                        document.getDocumentType(),
                        document.getPermissionLevel(),
                        document.getEffectiveFrom(),
                        document.getEffectiveTo(),
                        splitTags(document.getTags())),
                document.getCreatedAt(),
                document.getUpdatedAt());
    }

    private static List<String> splitTags(String tags) {
        if (tags == null || tags.isBlank()) {
            return List.of();
        }
        return List.of(tags.split(",")).stream()
                .map(String::trim)
                .filter(tag -> !tag.isBlank())
                .toList();
    }

    public record StorageSummary(
            String bucket,
            String objectKey,
            String checksum) {
    }

    public record DocumentMetadataResponse(
            String source,
            String businessDomain,
            String documentType,
            String permissionLevel,
            Instant effectiveFrom,
            Instant effectiveTo,
            List<String> tags) {
    }
}
