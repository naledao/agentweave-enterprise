package com.agentweave.springai.rag.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record VectorRagSearchRequest(
        @NotBlank String query,
        String businessDomain,
        String documentType,
        String permissionLevel,
        UUID documentId,
        @Min(1) @Max(20) Integer topK,
        Double similarityThreshold) {

    public int normalizedTopK() {
        return topK == null ? 5 : topK;
    }

    public double normalizedSimilarityThreshold() {
        if (similarityThreshold == null) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, similarityThreshold));
    }

    public String normalizedQuery() {
        return query == null ? "" : query.trim();
    }

    public String normalizedBusinessDomain() {
        return normalize(businessDomain);
    }

    public String normalizedDocumentType() {
        return normalize(documentType);
    }

    public String normalizedPermissionLevel() {
        return normalize(permissionLevel);
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
