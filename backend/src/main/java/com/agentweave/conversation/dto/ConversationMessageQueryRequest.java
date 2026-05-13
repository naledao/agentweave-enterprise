package com.agentweave.conversation.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record ConversationMessageQueryRequest(
        @Min(0)
        Integer page,

        @Min(1)
        @Max(100)
        Integer size) {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    public int pageNumber() {
        return page == null ? DEFAULT_PAGE : page;
    }

    public int pageSize() {
        if (size == null) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }
}
