package com.agentweave.conversation.dto;

public record CitationEventResponse(
        String documentId,
        String documentName,
        String chunkId,
        String title,
        String source,
        String snippet,
        Double score) {

    public CitationEventResponse(
            String documentId,
            String chunkId,
            String title,
            String snippet,
            Double score) {
        this(documentId, title, chunkId, title, null, snippet, score);
    }
}
