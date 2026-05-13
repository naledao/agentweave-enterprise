package com.agentweave.conversation.application;

import java.util.List;
import java.util.UUID;

public record ConversationPrompt(
        UUID conversationId,
        String title,
        String latestUserMessage,
        List<ConversationTurn> turns) {
}
