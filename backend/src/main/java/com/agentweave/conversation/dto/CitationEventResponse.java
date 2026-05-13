package com.agentweave.conversation.dto;

public record CitationEventResponse(
        String documentId,
        String chunkId,
        String title,
        String snippet,
        Double score) {
}
