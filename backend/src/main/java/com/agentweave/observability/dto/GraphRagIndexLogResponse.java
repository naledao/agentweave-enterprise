package com.agentweave.observability.dto;

import com.agentweave.graphrag.domain.GraphRagIndexLog;
import java.time.Instant;
import java.util.UUID;

public record GraphRagIndexLogResponse(
        UUID id,
        UUID documentId,
        String traceId,
        int entityCount,
        int relationshipCount,
        int chunkCount,
        int chunkEntityCount,
        boolean neo4jEnabled,
        long durationMs,
        String status,
        String errorMessage,
        Instant startedAt,
        Instant completedAt,
        Instant createdAt) {

    public static GraphRagIndexLogResponse from(GraphRagIndexLog entity) {
        return new GraphRagIndexLogResponse(
                entity.getId(),
                entity.getDocumentId(),
                entity.getTraceId(),
                entity.getEntityCount(),
                entity.getRelationshipCount(),
                entity.getChunkCount(),
                entity.getChunkEntityCount(),
                entity.isNeo4jEnabled(),
                entity.getDurationMs(),
                entity.getStatus().name(),
                entity.getErrorMessage(),
                entity.getStartedAt(),
                entity.getCompletedAt(),
                entity.getCreatedAt());
    }
}
