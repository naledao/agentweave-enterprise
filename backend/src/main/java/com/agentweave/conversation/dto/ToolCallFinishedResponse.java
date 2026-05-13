package com.agentweave.conversation.dto;

public record ToolCallFinishedResponse(
        String toolCallId,
        String status,
        Long latencyMs,
        String resultSummary) {
}
