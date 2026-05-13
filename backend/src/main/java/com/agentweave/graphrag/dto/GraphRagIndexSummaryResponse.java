package com.agentweave.graphrag.dto;

import java.time.Instant;

public record GraphRagIndexSummaryResponse(
        String status,
        int entityCount,
        int relationshipCount,
        int chunkCount,
        String errorMessage,
        String traceId,
        Instant indexedAt) {

    public static GraphRagIndexSummaryResponse pending() {
        return new GraphRagIndexSummaryResponse("pending", 0, 0, 0, null, null, null);
    }

    public static GraphRagIndexSummaryResponse processing(String traceId, int chunkCount) {
        return new GraphRagIndexSummaryResponse("processing", 0, 0, chunkCount, null, traceId, null);
    }

    public static GraphRagIndexSummaryResponse indexed(
            String traceId,
            int entityCount,
            int relationshipCount,
            int chunkCount,
            Instant indexedAt) {
        return new GraphRagIndexSummaryResponse("indexed", entityCount, relationshipCount, chunkCount, null, traceId, indexedAt);
    }

    public static GraphRagIndexSummaryResponse failed(
            String traceId,
            int entityCount,
            int relationshipCount,
            int chunkCount,
            String errorMessage) {
        return new GraphRagIndexSummaryResponse("failed", entityCount, relationshipCount, chunkCount, errorMessage, traceId, null);
    }
}
