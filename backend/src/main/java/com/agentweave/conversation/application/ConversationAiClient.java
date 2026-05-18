package com.agentweave.conversation.application;

import reactor.core.publisher.Flux;

public interface ConversationAiClient {

    ConversationAiResponse answer(ConversationPrompt prompt);

    Flux<ConversationAiChunk> streamAnswer(ConversationPrompt prompt);
}
