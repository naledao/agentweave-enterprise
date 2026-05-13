package com.agentweave.springai.rag;

import com.agentweave.shared.exception.BusinessException;
import com.agentweave.shared.exception.ErrorCode;
import java.util.Locale;

public enum RagRetrievalMode {
    VECTOR_ONLY,
    GRAPH_ONLY,
    HYBRID;

    public static RagRetrievalMode from(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return RagRetrievalMode.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_FAILED,
                    "Unsupported retrieval mode: " + value);
        }
    }

    public boolean usesVectorRetrieval() {
        return this == VECTOR_ONLY || this == HYBRID;
    }

    public boolean usesGraphRetrieval() {
        return this == GRAPH_ONLY || this == HYBRID;
    }
}
