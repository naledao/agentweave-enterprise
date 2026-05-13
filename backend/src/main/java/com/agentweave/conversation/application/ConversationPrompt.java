package com.agentweave.conversation.application;

import java.util.List;
import java.util.UUID;

public record ConversationPrompt(
        UUID conversationId,
        String title,
        String latestUserMessage,
        List<ConversationTurn> turns,
        RagPromptContext ragContext) {

    public ConversationPrompt(
            UUID conversationId,
            String title,
            String latestUserMessage,
            List<ConversationTurn> turns) {
        this(conversationId, title, latestUserMessage, turns, RagPromptContext.empty());
    }
}
