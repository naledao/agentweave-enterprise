package com.agentweave.graphrag.dto;

public record GraphRagRelationshipCandidate(
        String sourceName,
        String sourceType,
        String targetName,
        String targetType,
        String type,
        String description,
        double confidence) {
}
