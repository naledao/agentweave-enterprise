package com.agentweave.tool.dto;

import java.time.Instant;
import java.util.Map;

public record ToolExecutionResponse(
        String toolName,
        String status,
        Map<String, Object> result,
        Instant executedAt) {

    public static ToolExecutionResponse succeeded(String toolName, Map<String, Object> result) {
        return new ToolExecutionResponse(toolName, "SUCCEEDED", result, Instant.now());
    }
}
