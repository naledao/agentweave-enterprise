package com.agentweave.springai.rag.dto;

import java.util.List;
import java.util.Map;

public record VectorRagSearchResponse(
        String query,
        String retrievalMode,
        int topK,
        double similarityThreshold,
        Map<String, Object> filter,
        List<VectorRagCitationResponse> citations) {
}
