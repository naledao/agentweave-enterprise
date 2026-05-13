package com.agentweave.graphrag.domain;

import java.util.Locale;

public enum KnowledgeGraphRelationshipType {
    DEPENDS_ON,
    CALLS,
    OWNS,
    BELONGS_TO,
    MENTIONS,
    CAUSES,
    RESOLVES,
    RELATED_TO,
    AFFECTS,
    DOCUMENTED_BY;

    public static KnowledgeGraphRelationshipType from(String value) {
        if (value == null || value.isBlank()) {
            return RELATED_TO;
        }
        String normalized = value.trim()
                .replace(' ', '_')
                .replace('-', '_')
                .toUpperCase(Locale.ROOT);
        for (KnowledgeGraphRelationshipType candidate : values()) {
            if (candidate.name().equals(normalized)) {
                return candidate;
            }
        }
        return RELATED_TO;
    }
}
