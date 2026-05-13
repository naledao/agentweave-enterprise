package com.agentweave.conversation.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SendMessageResponse(
        UUID conversationId,
        UUID userMessageId,
        UUID assistantMessageId,
        String traceId,
        String answer) {

    public SendMessageResponse(
            UUID conversationId,
            UUID userMessageId,
            UUID assistantMessageId,
            String traceId) {
        this(conversationId, userMessageId, assistantMessageId, traceId, null);
    }
}
