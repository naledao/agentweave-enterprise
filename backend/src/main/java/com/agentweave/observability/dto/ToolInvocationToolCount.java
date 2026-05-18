package com.agentweave.observability.dto;

public record ToolInvocationToolCount(
        String toolCode,
        String toolName,
        String toolType,
        long count,
        long failed,
        long denied,
        long timeout,
        double averageDurationMs) {
}
