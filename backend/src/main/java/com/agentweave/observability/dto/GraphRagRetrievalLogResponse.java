package com.agentweave.observability.dto;

import com.agentweave.graphrag.domain.GraphRagRetrievalLog;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record GraphRagRetrievalLogResponse(
        UUID id,
        String traceId,
        UUID conversationId,
        UUID messageId,
        UUID workflowRunId,
        UUID workflowStepId,
        String query,
        String retrievalMode,
        String businessDomain,
        String permissionLevel,
        UUID documentId,
        int maxDepth,
        int maxPathCount,
        List<String> resolvedEntities,
        int matchedPathCount,
        int filteredPathCount,
        List<String> sourceChunkIds,
        String confidenceSummary,
        long durationMs,
        String status,
        String errorMessage,
        Instant startedAt,
        Instant completedAt,
        Instant createdAt) {

    public static GraphRagRetrievalLogResponse from(GraphRagRetrievalLog entity) {
        return new GraphRagRetrievalLogResponse(
                entity.getId(),
                entity.getTraceId(),
                entity.getConversationId(),
                entity.getMessageId(),
                entity.getWorkflowRunId(),
                entity.getWorkflowStepId(),
                entity.getQuery(),
                entity.getRetrievalMode(),
                entity.getBusinessDomain(),
                entity.getPermissionLevel(),
                entity.getDocumentId(),
                entity.getMaxDepth(),
                entity.getMaxPathCount(),
                entity.getResolvedEntities(),
                entity.getMatchedPathCount(),
                entity.getFilteredPathCount(),
                entity.getSourceChunkIds(),
                entity.getConfidenceSummary(),
                entity.getDurationMs(),
                entity.getStatus().name(),
                entity.getErrorMessage(),
                entity.getStartedAt(),
                entity.getCompletedAt(),
                entity.getCreatedAt());
    }
}
