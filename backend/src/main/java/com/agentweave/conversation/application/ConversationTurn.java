package com.agentweave.conversation.application;

public record ConversationTurn(
        String role,
        String content) {
}
