package com.agentweave.conversation.dto;

import com.agentweave.conversation.domain.ConversationEntity;
import com.agentweave.conversation.domain.ConversationStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;

public record ConversationDetailResponse(
        UUID id,
        String title,
        ConversationStatus status,
        int messageCount,
        String lastMessagePreview,
        Instant lastMessageAt,
        Instant createdAt,
        Instant updatedAt,
        List<ConversationMessageResponse> messages,
        int messagePage,
        int messageSize,
        long messageTotal,
        int messageTotalPages,
        String traceId) {

    public static ConversationDetailResponse from(
            ConversationEntity conversation,
            Page<ConversationMessageResponse> messages,
            String traceId) {
        ConversationSummaryResponse summary = ConversationSummaryResponse.from(conversation);
        return new ConversationDetailResponse(
                conversation.getId(),
                conversation.getTitle(),
                conversation.getStatus(),
                summary.messageCount(),
                summary.lastMessagePreview(),
                summary.lastMessageAt(),
                conversation.getCreatedAt(),
                conversation.getUpdatedAt(),
                messages.getContent(),
                messages.getNumber(),
                messages.getSize(),
                messages.getTotalElements(),
                messages.getTotalPages(),
                traceId);
    }
}
