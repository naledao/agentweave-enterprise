package com.agentweave.observability.dto;

public record ToolInvocationStatusCount(
        String status,
        long count) {
}
