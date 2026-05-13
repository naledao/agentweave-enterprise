package com.agentweave.graphrag.dto;

import java.util.List;

public record GraphRagEntityCandidate(
        String name,
        String type,
        String description,
        List<String> aliases,
        double confidence) {

    public GraphRagEntityCandidate {
        aliases = aliases == null ? List.of() : List.copyOf(aliases);
    }
}
