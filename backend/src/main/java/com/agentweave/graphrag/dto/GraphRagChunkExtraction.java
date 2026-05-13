package com.agentweave.graphrag.dto;

import java.util.List;
import java.util.UUID;

public record GraphRagChunkExtraction(
        UUID chunkId,
        List<GraphRagEntityCandidate> entities,
        List<GraphRagRelationshipCandidate> relationships) {

    public GraphRagChunkExtraction {
        entities = entities == null ? List.of() : List.copyOf(entities);
        relationships = relationships == null ? List.of() : List.copyOf(relationships);
    }
}
