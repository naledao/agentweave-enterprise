package com.agentweave.springai.rag;

import com.agentweave.shared.security.CurrentUser;
import java.util.UUID;

public record RagRoutingRequest(
        String query,
        UUID conversationId,
        String businessDomain,
        String documentType,
        String permissionLevel,
        RagRetrievalMode requestedRetrievalMode,
        CurrentUser currentUser) {

    public String normalizedQuery() {
        return normalize(query);
    }

    public String normalizedBusinessDomain() {
        return normalize(businessDomain);
    }

    public String normalizedDocumentType() {
        return normalize(documentType);
    }

    public String normalizedPermissionLevel() {
        return normalize(permissionLevel);
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
