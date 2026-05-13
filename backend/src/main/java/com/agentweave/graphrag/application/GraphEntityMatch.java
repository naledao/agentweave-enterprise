package com.agentweave.graphrag.application;

import com.agentweave.graphrag.domain.KnowledgeGraphEntity;

public record GraphEntityMatch(
        KnowledgeGraphEntity entity,
        double score,
        String matchedText) {

    public GraphEntityMatch {
        score = Math.max(0.0d, Math.min(1.0d, score));
    }

    public boolean isStrong() {
        return score >= 0.9d;
    }
}
