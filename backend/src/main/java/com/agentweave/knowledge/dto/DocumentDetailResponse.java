package com.agentweave.knowledge.dto;

import com.agentweave.graphrag.dto.GraphRagIndexSummaryResponse;
import com.agentweave.knowledge.domain.DocumentChunkEntity;
import com.agentweave.knowledge.domain.DocumentEntity;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DocumentDetailResponse(
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
        DocumentResponse.StorageSummary storage,
        DocumentResponse.DocumentMetadataResponse metadata,
        Instant createdAt,
        Instant updatedAt,
        GraphRagIndexSummaryResponse graphRag,
        List<DocumentChunkResponse> chunks,
        List<DocumentCitationRecordResponse> citationRecords) {

    public static DocumentDetailResponse from(
            DocumentEntity document,
            List<DocumentChunkEntity> chunks,
            GraphRagIndexSummaryResponse graphRag,
            List<DocumentCitationRecordResponse> citationRecords) {
        DocumentResponse summary = DocumentResponse.from(document, chunks.size());
        return new DocumentDetailResponse(
                summary.documentId(),
                summary.filename(),
                summary.contentType(),
                summary.fileSize(),
                summary.uploadedBy(),
                summary.status(),
                summary.errorMessage(),
                summary.traceId(),
                summary.chunkCount(),
                summary.reindexCount(),
                summary.indexedAt(),
                summary.storage(),
                summary.metadata(),
                summary.createdAt(),
                summary.updatedAt(),
                graphRag,
                chunks.stream()
                        .map(DocumentChunkResponse::from)
                        .toList(),
                citationRecords == null ? List.of() : List.copyOf(citationRecords));
    }
}
