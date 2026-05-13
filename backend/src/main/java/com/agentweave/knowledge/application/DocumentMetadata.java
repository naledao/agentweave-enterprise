package com.agentweave.knowledge.application;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record DocumentMetadata(
        UUID documentId,
        UUID chunkId,
        String source,
        String businessDomain,
        String documentType,
        String permissionLevel,
        Instant createdAt,
        Instant effectiveFrom,
        Instant effectiveTo,
        List<String> tags) {

    public Map<String, Object> toMap() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("documentId", documentId.toString());
        metadata.put("chunkId", chunkId.toString());
        metadata.put("source", source);
        metadata.put("businessDomain", businessDomain);
        metadata.put("documentType", documentType);
        metadata.put("permissionLevel", permissionLevel);
        metadata.put("createdAt", formatInstant(createdAt));
        metadata.put("effectiveFrom", formatInstant(effectiveFrom));
        metadata.put("effectiveTo", formatInstant(effectiveTo));
        metadata.put("tags", tags == null ? List.of() : List.copyOf(tags));
        return metadata;
    }

    private String formatInstant(Instant instant) {
        return instant == null ? null : instant.toString();
    }
}
