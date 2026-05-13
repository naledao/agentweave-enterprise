package com.agentweave.conversation.application;

public record ConversationAiResponse(
        String content,
        String provider,
        String model,
        Integer promptTokens,
        Integer completionTokens) {
}
