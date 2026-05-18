package com.agentweave.langchain4j.agent;

import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.output.TokenUsage;
import org.springframework.stereotype.Component;

@Component
public class AgentModelObservation implements ChatModelListener {

    private final ThreadLocal<TokenUsage> tokenUsage = new ThreadLocal<>();

    @Override
    public void onResponse(ChatModelResponseContext responseContext) {
        if (responseContext.chatResponse() != null) {
            tokenUsage.set(responseContext.chatResponse().tokenUsage());
        }
    }

    @Override
    public void onError(ChatModelErrorContext errorContext) {
        tokenUsage.remove();
    }

    public TokenUsage currentTokenUsage() {
        return tokenUsage.get();
    }

    public void clear() {
        tokenUsage.remove();
    }
}
