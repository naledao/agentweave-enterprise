package com.agentweave.graphrag.domain;

import java.util.Locale;

public enum GraphRagIndexStatus {
    PROCESSING,
    INDEXED,
    FAILED,
    SKIPPED;

    public String apiValue() {
        return name().toLowerCase(Locale.ROOT);
    }
}
