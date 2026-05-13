package com.agentweave.knowledge.messaging.event;

public enum DocumentProcessingEventType {

    DOCUMENT_UPLOADED("document.uploaded"),
    DOCUMENT_PARSED("document.parsed"),
    DOCUMENT_CHUNKED("document.chunked"),
    DOCUMENT_VECTOR_INDEXED("document.vector-indexed"),
    DOCUMENT_REINDEX_REQUESTED("document.reindex.requested"),
    DOCUMENT_PROCESSING_FAILED("document.processing.failed");

    private final String routingKey;

    DocumentProcessingEventType(String routingKey) {
        this.routingKey = routingKey;
    }

    public String routingKey() {
        return routingKey;
    }
}
