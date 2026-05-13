package com.agentweave.graphrag.dto;

import java.util.UUID;

public record GraphRagExtractionCommand(
        UUID documentId,
        UUID chunkId,
        String chunkContent,
        String businessDomain,
        String documentType,
        String permissionLevel) {
}
