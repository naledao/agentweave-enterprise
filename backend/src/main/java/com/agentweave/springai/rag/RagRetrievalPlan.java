package com.agentweave.springai.rag;

import java.util.LinkedHashMap;
import java.util.Map;

public record RagRetrievalPlan(
        RagRetrievalMode retrievalMode,
        int vectorTopK,
        double similarityThreshold,
        int graphMaxDepth,
        Map<String, Object> metadataFilter,
        Map<String, Object> graphFilter,
        String routingReason) {

    public RagRetrievalPlan {
        metadataFilter = copy(metadataFilter);
        graphFilter = copy(graphFilter);
    }

    public boolean shouldSearchVector() {
        return retrievalMode.usesVectorRetrieval();
    }

    public boolean shouldSearchGraph() {
        return retrievalMode.usesGraphRetrieval();
    }

    private static Map<String, Object> copy(Map<String, Object> filter) {
        if (filter == null || filter.isEmpty()) {
            return Map.of();
        }
        return Map.copyOf(new LinkedHashMap<>(filter));
    }
}
