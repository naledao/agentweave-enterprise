package com.agentweave.conversation.dto;

import com.agentweave.conversation.domain.MessageStatus;

public enum ConversationMessageStatusResponse {
    PENDING,
    SUCCEEDED,
    STREAMING,
    FAILED,
    CANCELLED;

    public static ConversationMessageStatusResponse from(MessageStatus status) {
        return switch (status) {
            case PENDING -> PENDING;
            case SUCCEEDED -> SUCCEEDED;
            case STREAMING -> STREAMING;
            case FAILED -> FAILED;
            case CANCELLED -> CANCELLED;
        };
    }
}
