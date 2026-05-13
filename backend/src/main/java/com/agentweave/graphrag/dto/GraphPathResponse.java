package com.agentweave.graphrag.dto;

import java.util.List;

public record GraphPathResponse(
        String pathId,
        int depth,
        List<String> entities,
        List<String> relationships,
        List<String> sourceChunkIds,
        Double confidence) {

    public GraphPathResponse {
        entities = entities == null ? List.of() : List.copyOf(entities);
        relationships = relationships == null ? List.of() : List.copyOf(relationships);
        sourceChunkIds = sourceChunkIds == null ? List.of() : List.copyOf(sourceChunkIds);
    }
}
