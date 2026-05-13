package com.agentweave.knowledge.messaging.config;

import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "agentweave.document-pipeline.rabbitmq", name = "enabled", havingValue = "true")
public class RabbitMqDocumentMessageConfig {

    @Bean
    MessageConverter documentPipelineMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
