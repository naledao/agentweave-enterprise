package com.agentweave.conversation.dto;

import com.agentweave.graphrag.dto.GraphPathResponse;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SendMessageResponse(
        UUID conversationId,
        UUID userMessageId,
        UUID assistantMessageId,
        String traceId,
        String answer,
        String retrievalMode,
        List<CitationEventResponse> citations,
        List<GraphPathResponse> graphPaths) {

    public SendMessageResponse(
            UUID conversationId,
            UUID userMessageId,
            UUID assistantMessageId,
            String traceId) {
        this(conversationId, userMessageId, assistantMessageId, traceId, null, null, null, null);
    }

    public SendMessageResponse(
            UUID conversationId,
            UUID userMessageId,
            UUID assistantMessageId,
            String traceId,
            String answer) {
        this(conversationId, userMessageId, assistantMessageId, traceId, answer, null, null, null);
    }
}
