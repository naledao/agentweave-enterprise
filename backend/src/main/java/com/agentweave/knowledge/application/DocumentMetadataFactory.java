package com.agentweave.knowledge.application;

import com.agentweave.knowledge.domain.DocumentEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class DocumentMetadataFactory {

    public DocumentMetadata forChunk(DocumentEntity document, UUID chunkId) {
        return new DocumentMetadata(
                document.getId(),
                chunkId,
                document.getSource(),
                document.getBusinessDomain(),
                document.getDocumentType(),
                document.getPermissionLevel(),
                document.getCreatedAt(),
                document.getEffectiveFrom(),
                document.getEffectiveTo(),
                splitTags(document.getTags()));
    }

    private List<String> splitTags(String tags) {
        if (tags == null || tags.isBlank()) {
            return List.of();
        }
        return List.of(tags.split(",")).stream()
                .map(String::trim)
                .filter(tag -> !tag.isBlank())
                .toList();
    }
}
