package com.agentweave.conversation.dto;

import com.agentweave.conversation.domain.ConversationMessageEntity;
import com.agentweave.conversation.domain.MessageRole;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ConversationMessageResponse(
        UUID id,
        UUID conversationId,
        MessageRole role,
        String content,
        ConversationMessageStatusResponse status,
        String errorCode,
        String errorMessage,
        String metadata,
        String traceId,
        List<CitationEventResponse> citations,
        List<ToolCallFinishedResponse> toolCalls,
        Instant createdAt) {

    public static ConversationMessageResponse from(ConversationMessageEntity message) {
        return new ConversationMessageResponse(
                message.getId(),
                message.getConversation().getId(),
                message.getRole(),
                message.getContent(),
                ConversationMessageStatusResponse.from(message.getStatus()),
                message.getErrorCode(),
                message.getErrorMessage(),
                message.getMetadata(),
                message.getTraceId(),
                List.of(),
                List.of(),
                message.getCreatedAt());
    }
}
