package com.agentweave.springai.rag.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.AssertTrue;
import java.time.Instant;
import java.util.UUID;

public record VectorRagSearchRequest(
        @NotBlank String query,
        String businessDomain,
        String documentType,
        String permissionLevel,
        UUID documentId,
        Instant effectiveFrom,
        Instant effectiveTo,
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

    public String normalizedTimeRange() {
        if (effectiveFrom == null && effectiveTo == null) {
            return null;
        }
        String from = effectiveFrom == null ? "*" : effectiveFrom.toString();
        String to = effectiveTo == null ? "*" : effectiveTo.toString();
        return from + ".." + to;
    }

    @AssertTrue(message = "effectiveFrom must be before effectiveTo")
    public boolean isTimeRangeValid() {
        return effectiveFrom == null || effectiveTo == null || !effectiveFrom.isAfter(effectiveTo);
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
