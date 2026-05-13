package com.agentweave.knowledge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

public record CreateDocumentRequest(
        @NotBlank @Size(max = 160) String source,
        @NotBlank @Size(max = 120) String businessDomain,
        @NotBlank @Size(max = 80) String documentType,
        @NotBlank @Size(max = 80) String permissionLevel,
        Instant effectiveFrom,
        Instant effectiveTo,
        List<@Size(max = 40) String> tags) {

    public List<String> normalizedTags() {
        if (tags == null) {
            return List.of();
        }
        return tags.stream()
                .filter(tag -> tag != null && !tag.isBlank())
                .map(String::trim)
                .distinct()
                .limit(20)
                .toList();
    }
}
