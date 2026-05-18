package com.agentweave.observability.dto;

import com.agentweave.springai.rag.domain.RagRetrievalLog;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record RagRetrievalResponse(
        UUID id,
        String traceId,
        UUID conversationId,
        UUID messageId,
        UUID workflowRunId,
        UUID workflowStepId,
        String query,
        String retrievalMode,
        Map<String, Object> metadataFilter,
        String businessDomain,
        String documentType,
        String permissionLevel,
        String timeRange,
        UUID documentId,
        int topK,
        double similarityThreshold,
        List<String> matchedChunkIds,
        List<Map<String, Object>> citationSummaries,
        String scoreSummary,
        int citationCount,
        long durationMs,
        String status,
        String errorMessage,
        Instant startedAt,
        Instant completedAt,
        Instant createdAt) {

    public static RagRetrievalResponse from(RagRetrievalLog entity) {
        return new RagRetrievalResponse(
                entity.getId(),
                entity.getTraceId(),
                entity.getConversationId(),
                entity.getMessageId(),
                entity.getWorkflowRunId(),
                entity.getWorkflowStepId(),
                entity.getQuery(),
                entity.getRetrievalMode(),
                entity.getMetadataFilter(),
                entity.getBusinessDomain(),
                entity.getDocumentType(),
                entity.getPermissionLevel(),
                entity.getTimeRange(),
                entity.getDocumentId(),
                entity.getTopK(),
                entity.getSimilarityThreshold(),
                entity.getMatchedChunkIds(),
                entity.getCitationSummaries(),
                entity.getScoreSummary(),
                entity.getCitationCount(),
                entity.getDurationMs(),
                entity.getStatus().name(),
                entity.getErrorMessage(),
                entity.getStartedAt(),
                entity.getCompletedAt(),
                entity.getCreatedAt());
    }
}
