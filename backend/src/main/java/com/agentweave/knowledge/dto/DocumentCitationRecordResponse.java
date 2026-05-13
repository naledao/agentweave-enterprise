package com.agentweave.knowledge.dto;

import com.agentweave.conversation.domain.ConversationMessageEntity;
import java.time.Instant;
import java.util.UUID;

public record DocumentCitationRecordResponse(
        UUID conversationId,
        UUID messageId,
        String messagePreview,
        String traceId,
        Instant createdAt) {

    public static DocumentCitationRecordResponse from(ConversationMessageEntity message) {
        return new DocumentCitationRecordResponse(
                message.getConversation().getId(),
                message.getId(),
                preview(message.getContent()),
                message.getTraceId(),
                message.getCreatedAt());
    }

    private static String preview(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }
        String normalized = content.trim().replaceAll("\\s+", " ");
        return normalized.length() <= 160 ? normalized : normalized.substring(0, 160);
    }
}
