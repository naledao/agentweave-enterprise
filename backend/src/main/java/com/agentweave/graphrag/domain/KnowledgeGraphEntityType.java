package com.agentweave.graphrag.domain;

import java.util.Locale;

public enum KnowledgeGraphEntityType {
    SERVICE,
    API,
    DATABASE,
    ERROR_CODE,
    TICKET,
    PRODUCT,
    MODULE,
    PERSON,
    TEAM,
    DOCUMENT,
    CONCEPT;

    public static KnowledgeGraphEntityType from(String value) {
        if (value == null || value.isBlank()) {
            return CONCEPT;
        }
        String normalized = value.trim()
                .replace(' ', '_')
                .replace('-', '_')
                .toUpperCase(Locale.ROOT);
        for (KnowledgeGraphEntityType candidate : values()) {
            if (candidate.name().equals(normalized)) {
                return candidate;
            }
        }
        return CONCEPT;
    }
}
