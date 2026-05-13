package com.agentweave.knowledge.messaging.config;

import com.agentweave.knowledge.messaging.event.DocumentProcessingEventType;
import java.util.Map;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "agentweave.document-pipeline.rabbitmq", name = "enabled", havingValue = "true")
public class RabbitMqDocumentTopologyConfig {

    @Bean
    Declarables documentPipelineDeclarables(RabbitMqDocumentProperties properties) {
        TopicExchange documentExchange = new TopicExchange(properties.exchange(), true, false);
        TopicExchange retryExchange = new TopicExchange(properties.retryExchange(), true, false);
        TopicExchange deadLetterExchange = new TopicExchange(properties.deadLetterExchange(), true, false);

        Queue parseQueue = businessQueue(properties.parseQueue(), properties.deadLetterExchange(), properties.parseDlq());
        Queue chunkQueue = businessQueue(properties.chunkQueue(), properties.deadLetterExchange(), properties.chunkDlq());
        Queue vectorIndexQueue = businessQueue(
                properties.vectorIndexQueue(),
                properties.deadLetterExchange(),
                properties.vectorIndexDlq());
        Queue graphRagIndexQueue = businessQueue(
                properties.graphRagIndexQueue(),
                properties.deadLetterExchange(),
                properties.graphRagIndexDlq());
        Queue reindexQueue = businessQueue(
                properties.reindexQueue(),
                properties.deadLetterExchange(),
                properties.reindexDlq());

        Queue parseDlq = dlq(properties.parseDlq());
        Queue chunkDlq = dlq(properties.chunkDlq());
        Queue vectorIndexDlq = dlq(properties.vectorIndexDlq());
        Queue graphRagIndexDlq = dlq(properties.graphRagIndexDlq());
        Queue reindexDlq = dlq(properties.reindexDlq());

        return new Declarables(
                documentExchange,
                retryExchange,
                deadLetterExchange,
                parseQueue,
                chunkQueue,
                vectorIndexQueue,
                graphRagIndexQueue,
                reindexQueue,
                parseDlq,
                chunkDlq,
                vectorIndexDlq,
                graphRagIndexDlq,
                reindexDlq,
                bind(parseQueue, documentExchange, DocumentProcessingEventType.DOCUMENT_UPLOADED.routingKey()),
                bind(chunkQueue, documentExchange, DocumentProcessingEventType.DOCUMENT_PARSED.routingKey()),
                bind(vectorIndexQueue, documentExchange, DocumentProcessingEventType.DOCUMENT_CHUNKED.routingKey()),
                bind(graphRagIndexQueue, documentExchange, DocumentProcessingEventType.DOCUMENT_VECTOR_INDEXED.routingKey()),
                bind(reindexQueue, documentExchange, DocumentProcessingEventType.DOCUMENT_REINDEX_REQUESTED.routingKey()),
                bind(parseDlq, deadLetterExchange, properties.parseDlq()),
                bind(chunkDlq, deadLetterExchange, properties.chunkDlq()),
                bind(vectorIndexDlq, deadLetterExchange, properties.vectorIndexDlq()),
                bind(graphRagIndexDlq, deadLetterExchange, properties.graphRagIndexDlq()),
                bind(reindexDlq, deadLetterExchange, properties.reindexDlq()));
    }

    private Queue businessQueue(String name, String deadLetterExchange, String deadLetterRoutingKey) {
        return QueueBuilder.durable(name)
                .withArguments(Map.of(
                        "x-dead-letter-exchange", deadLetterExchange,
                        "x-dead-letter-routing-key", deadLetterRoutingKey))
                .build();
    }

    private Queue dlq(String name) {
        return QueueBuilder.durable(name).build();
    }

    private Binding bind(Queue queue, TopicExchange exchange, String routingKey) {
        return BindingBuilder.bind(queue).to(exchange).with(routingKey);
    }
}
