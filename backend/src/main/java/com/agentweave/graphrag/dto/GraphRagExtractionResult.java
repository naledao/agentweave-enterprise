package com.agentweave.graphrag.dto;

import java.util.List;

public record GraphRagExtractionResult(
        List<GraphRagEntityCandidate> entities,
        List<GraphRagRelationshipCandidate> relationships) {

    public GraphRagExtractionResult {
        entities = entities == null ? List.of() : List.copyOf(entities);
        relationships = relationships == null ? List.of() : List.copyOf(relationships);
    }
}
