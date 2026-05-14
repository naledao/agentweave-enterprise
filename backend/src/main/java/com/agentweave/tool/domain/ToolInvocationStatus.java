package com.agentweave.tool.domain;

import com.agentweave.shared.exception.BusinessException;
import com.agentweave.shared.exception.ErrorCode;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Locale;

public enum ToolInvocationStatus {
    RUNNING("running"),
    SUCCESS("success"),
    FAILED("failed"),
    DENIED("denied"),
    TIMEOUT("timeout");

    private final String value;

    ToolInvocationStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    public static ToolInvocationStatus fromQuery(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        try {
            return ToolInvocationStatus.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "invalid invocation status");
        }
    }
}
