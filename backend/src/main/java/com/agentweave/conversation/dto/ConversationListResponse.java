package com.agentweave.conversation.dto;

import java.util.List;
import org.springframework.data.domain.Page;

public record ConversationListResponse(
        List<ConversationSummaryResponse> items,
        int page,
        int size,
        long total,
        int totalPages) {

    public static ConversationListResponse from(Page<ConversationSummaryResponse> conversations) {
        return new ConversationListResponse(
                conversations.getContent(),
                conversations.getNumber(),
                conversations.getSize(),
                conversations.getTotalElements(),
                conversations.getTotalPages());
    }
}
