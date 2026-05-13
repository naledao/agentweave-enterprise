package com.agentweave.conversation.dto;

import com.agentweave.conversation.domain.ConversationEntity;
import com.agentweave.conversation.domain.ConversationStatus;
import java.time.Instant;
import java.util.UUID;

public record ConversationSummaryResponse(
        UUID id,
        String title,
        ConversationStatus status,
        int messageCount,
        String lastMessagePreview,
        Instant lastMessageAt,
        Instant createdAt,
        Instant updatedAt) {

    public static ConversationSummaryResponse from(ConversationEntity conversation) {
        return new ConversationSummaryResponse(
                conversation.getId(),
                conversation.getTitle(),
                conversation.getStatus(),
                conversation.getMessageCount(),
                conversation.getLastMessagePreview(),
                conversation.getLastMessageAt(),
                conversation.getCreatedAt(),
                conversation.getUpdatedAt());
    }
}
