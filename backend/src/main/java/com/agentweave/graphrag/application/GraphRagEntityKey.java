package com.agentweave.graphrag.application;

import com.agentweave.graphrag.domain.KnowledgeGraphEntityType;

public record GraphRagEntityKey(
        String normalizedName,
        KnowledgeGraphEntityType type) {
}
