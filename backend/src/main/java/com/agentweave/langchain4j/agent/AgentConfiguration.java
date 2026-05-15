package com.agentweave.langchain4j.agent;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AgentConfiguration {

    @Value("${langchain4j.open-ai.chat-model.api-key}")
    private String apiKey;

    @Value("${langchain4j.open-ai.chat-model.model-name}")
    private String modelName;

    @Value("${langchain4j.open-ai.chat-model.base-url}")
    private String baseUrl;

    @Bean
    public ChatModel agentChatModel() {
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .baseUrl(baseUrl)
                .build();
    }

    @Bean
    public PlannerAgent plannerAgent(ChatModel agentChatModel) {
        return AiServices.builder(PlannerAgent.class)
                .chatModel(agentChatModel)
                .build();
    }

    @Bean
    public ExecutorAgent executorAgent(ChatModel agentChatModel) {
        return AiServices.builder(ExecutorAgent.class)
                .chatModel(agentChatModel)
                .build();
    }

    @Bean
    public ReviewerAgent reviewerAgent(ChatModel agentChatModel) {
        return AiServices.builder(ReviewerAgent.class)
                .chatModel(agentChatModel)
                .build();
    }
}