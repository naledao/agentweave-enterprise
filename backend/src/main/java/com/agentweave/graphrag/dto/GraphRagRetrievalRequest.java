package com.agentweave.graphrag.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;

public record GraphRagRetrievalRequest(
        String query,
        String businessDomain,
        String permissionLevel,
        UUID documentId,
        @Min(1) @Max(3) Integer maxDepth,
        @Min(1) @Max(20) Integer maxPathCount) {

    public GraphRagRetrievalRequest {
        maxDepth = maxDepth == null ? 2 : Math.max(1, Math.min(3, maxDepth));
        maxPathCount = maxPathCount == null ? 5 : Math.max(1, Math.min(20, maxPathCount));
    }

    public String normalizedQuery() {
        return query == null ? "" : query.trim();
    }

    public String normalizedBusinessDomain() {
        return normalize(businessDomain);
    }

    public String normalizedPermissionLevel() {
        return normalize(permissionLevel);
    }

    public String normalizedDocumentId() {
        return documentId == null ? null : documentId.toString();
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
