package com.agentweave.knowledge.domain;

public enum DocumentStatus {
    UPLOADED,
    PARSING,
    CLEANING,
    CHUNKING,
    EMBEDDING,
    INDEXED,
    FAILED
}
