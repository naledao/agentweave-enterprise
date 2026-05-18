package com.agentweave.tool.domain;

import com.agentweave.shared.exception.BusinessException;
import com.agentweave.shared.exception.ErrorCode;
import java.util.Locale;

public enum ToolType {

    BUSINESS_QUERY,
    LOG_SEARCH,
    DATABASE_READ,
    ENDPOINT_STATUS,
    NOTIFICATION,
    MCP_RESOURCE,
    SCRIPT,
    UNKNOWN;

    public static ToolType fromQuery(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        try {
            return ToolType.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "invalid tool type");
        }
    }
}
