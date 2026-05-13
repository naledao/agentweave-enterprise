package com.agentweave.graphrag.dto;

import java.util.List;

public record GraphRagRetrievalResponse(
        List<GraphPathResponse> graphPaths,
        List<String> resolvedEntities,
        List<String> sourceChunkIds,
        String confidenceSummary,
        int matchedPathCount,
        int filteredPathCount) {

    public GraphRagRetrievalResponse {
        graphPaths = graphPaths == null ? List.of() : List.copyOf(graphPaths);
        resolvedEntities = resolvedEntities == null ? List.of() : List.copyOf(resolvedEntities);
        sourceChunkIds = sourceChunkIds == null ? List.of() : List.copyOf(sourceChunkIds);
        confidenceSummary = confidenceSummary == null ? "" : confidenceSummary;
    }

    public static GraphRagRetrievalResponse empty() {
        return new GraphRagRetrievalResponse(List.of(), List.of(), List.of(), "count=0", 0, 0);
    }
}
