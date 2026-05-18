package com.agentweave.observability.application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class ModelProviderHealthIndicator implements HealthIndicator {

    private final String chatModel;
    private final String embeddingModel;
    private final String openAiBaseUrl;
    private final String ollamaBaseUrl;
    private final String openAiApiKey;

    public ModelProviderHealthIndicator(
            @Value("${spring.ai.openai.chat.options.model:}") String chatModel,
            @Value("${spring.ai.ollama.embedding.options.model:${spring.ai.ollama.embedding.model:}}") String embeddingModel,
            @Value("${spring.ai.openai.base-url:}") String openAiBaseUrl,
            @Value("${spring.ai.ollama.base-url:}") String ollamaBaseUrl,
            @Value("${spring.ai.openai.api-key:}") String openAiApiKey) {
        this.chatModel = chatModel;
        this.embeddingModel = embeddingModel;
        this.openAiBaseUrl = openAiBaseUrl;
        this.ollamaBaseUrl = ollamaBaseUrl;
        this.openAiApiKey = openAiApiKey;
    }

    @Override
    public Health health() {
        Health.Builder builder = hasRequiredConfiguration() ? Health.up() : Health.down();
        return builder
                .withDetail("chatProvider", "openai")
                .withDetail("chatModel", safeDetail(chatModel))
                .withDetail("embeddingProvider", "ollama")
                .withDetail("embeddingModel", safeDetail(embeddingModel))
                .withDetail("openAiBaseUrl", safeDetail(openAiBaseUrl))
                .withDetail("ollamaBaseUrl", safeDetail(ollamaBaseUrl))
                .withDetail("apiKeyConfigured", hasText(openAiApiKey))
                .build();
    }

    private boolean hasRequiredConfiguration() {
        return hasText(chatModel)
                && hasText(embeddingModel)
                && hasText(openAiBaseUrl)
                && hasText(ollamaBaseUrl)
                && hasText(openAiApiKey);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String safeDetail(String value) {
        return hasText(value) ? value.trim() : "missing";
    }
}
