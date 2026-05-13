package com.agentweave.conversation.dto;

import com.agentweave.graphrag.dto.GraphPathResponse;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SseEventPayload(
        String eventId,
        UUID conversationId,
        UUID messageId,
        String traceId,
        Instant timestamp,
        Instant createdAt,
        String delta,
        String toolCallId,
        String toolName,
        String inputSummary,
        String status,
        Long latencyMs,
        String resultSummary,
        String documentId,
        String documentName,
        String chunkId,
        String title,
        String source,
        String snippet,
        Double score,
        String workflowRunId,
        String stepName,
        String code,
        String message,
        GraphPathResponse graphPath) {
}
