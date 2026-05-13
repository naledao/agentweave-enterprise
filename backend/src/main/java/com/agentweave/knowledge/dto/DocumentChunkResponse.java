package com.agentweave.knowledge.dto;

import com.agentweave.knowledge.domain.DocumentChunkEntity;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

public record DocumentChunkResponse(
        UUID chunkId,
        int chunkIndex,
        String content,
        int contentLength,
        String status,
        String errorMessage,
        UUID vectorId,
        Instant embeddedAt,
        String traceId,
        Instant createdAt,
        Instant updatedAt) {

    public static DocumentChunkResponse from(DocumentChunkEntity chunk) {
        return new DocumentChunkResponse(
                chunk.getId(),
                chunk.getChunkIndex(),
                chunk.getContent(),
                chunk.getContentLength(),
                chunk.getStatus().name().toLowerCase(Locale.ROOT),
                chunk.getErrorMessage(),
                chunk.getVectorId(),
                chunk.getEmbeddedAt(),
                chunk.getTraceId(),
                chunk.getCreatedAt(),
                chunk.getUpdatedAt());
    }
}
