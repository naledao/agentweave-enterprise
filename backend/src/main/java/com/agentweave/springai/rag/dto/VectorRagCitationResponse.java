package com.agentweave.springai.rag.dto;

import java.util.Map;

public record VectorRagCitationResponse(
        String documentId,
        String chunkId,
        String source,
        String businessDomain,
        String documentType,
        String permissionLevel,
        Double score,
        String snippet,
        Map<String, Object> metadata) {
}
