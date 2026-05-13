package com.agentweave.knowledge.application;

public record StoredDocumentObject(
        String bucket,
        String objectKey,
        String checksum,
        long size) {
}
