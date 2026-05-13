package com.agentweave.graphrag.domain;

import java.util.Locale;

public enum GraphRagIndexStatus {
    PROCESSING,
    INDEXED,
    FAILED;

    public String apiValue() {
        return name().toLowerCase(Locale.ROOT);
    }
}
