package com.agentweave.springai.chat;

import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatClientConfig {

    @Bean
    ChatMemory conversationChatMemory() {
        return MessageWindowChatMemory.builder()
                .maxMessages(40)
                .build();
    }

    @Bean
    MessageChatMemoryAdvisor messageChatMemoryAdvisor(ChatMemory conversationChatMemory) {
        return MessageChatMemoryAdvisor.builder(conversationChatMemory)
                .build();
    }
}
