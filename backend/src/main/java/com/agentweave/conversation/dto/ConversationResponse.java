package com.agentweave.conversation.dto;

import com.agentweave.conversation.domain.ConversationEntity;
import com.agentweave.conversation.domain.ConversationStatus;
import java.time.Instant;
import java.util.UUID;

public record ConversationResponse(
        UUID id,
        String title,
        ConversationStatus status,
        int messageCount,
        Instant createdAt,
        Instant updatedAt,
        String traceId) {

    public static ConversationResponse from(ConversationEntity conversation, String traceId) {
        return new ConversationResponse(
                conversation.getId(),
                conversation.getTitle(),
                conversation.getStatus(),
                conversation.getMessageCount(),
                conversation.getCreatedAt(),
                conversation.getUpdatedAt(),
                traceId);
    }
}
