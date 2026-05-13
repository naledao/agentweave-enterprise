package com.agentweave.knowledge.application;

import com.agentweave.knowledge.domain.DocumentEntity;

public record DocumentChunkingResult(
        DocumentEntity document,
        int chunkCount) {

    public DocumentChunkingResult {
        if (document == null) {
            throw new IllegalArgumentException("document must not be null");
        }
        if (chunkCount < 0) {
            throw new IllegalArgumentException("chunkCount must not be negative");
        }
    }
}
