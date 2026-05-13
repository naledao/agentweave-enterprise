package com.agentweave.conversation.dto;

import java.util.UUID;

public record CancelMessageResponse(
        UUID conversationId,
        UUID messageId,
        ConversationMessageStatusResponse status,
        String traceId) {
}
