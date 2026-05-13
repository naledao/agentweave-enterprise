package com.agentweave.knowledge.messaging.publisher;

import com.agentweave.knowledge.messaging.config.RabbitMqDocumentProperties;
import com.agentweave.knowledge.messaging.event.DocumentProcessingEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "agentweave.document-pipeline.rabbitmq", name = "enabled", havingValue = "true")
public class DocumentProcessingPublisher {

    public static final String EVENT_ID_HEADER = "x-event-id";
    public static final String EVENT_TYPE_HEADER = "x-event-type";
    public static final String TRACE_ID_HEADER = "x-trace-id";
    public static final String DOCUMENT_ID_HEADER = "x-document-id";
    public static final String TRIGGERED_BY_HEADER = "x-triggered-by";

    private static final Logger log = LoggerFactory.getLogger(DocumentProcessingPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final RabbitMqDocumentProperties properties;

    public DocumentProcessingPublisher(
            RabbitTemplate rabbitTemplate,
            RabbitMqDocumentProperties properties) {
        this.rabbitTemplate = rabbitTemplate;
        this.properties = properties;
    }

    public void publish(DocumentProcessingEvent event) {
        try {
            rabbitTemplate.convertAndSend(
                    properties.exchange(),
                    event.eventType(),
                    event,
                    messageHeaders(event));
            log.info(
                    "Document processing event published: eventId={}, eventType={}, documentId={}, traceId={}",
                    event.eventId(),
                    event.eventType(),
                    event.documentId(),
                    event.traceId());
        } catch (AmqpException ex) {
            log.warn(
                    "Document processing event publish failed: eventId={}, eventType={}, documentId={}, traceId={}, error={}",
                    event.eventId(),
                    event.eventType(),
                    event.documentId(),
                    event.traceId(),
                    ex.getMessage(),
                    ex);
            throw ex;
        }
    }

    private MessagePostProcessor messageHeaders(DocumentProcessingEvent event) {
        return message -> {
            message.getMessageProperties().setHeader(EVENT_ID_HEADER, event.eventId().toString());
            message.getMessageProperties().setHeader(EVENT_TYPE_HEADER, event.eventType());
            message.getMessageProperties().setHeader(TRACE_ID_HEADER, event.traceId());
            message.getMessageProperties().setHeader(DOCUMENT_ID_HEADER, event.documentId().toString());
            message.getMessageProperties().setHeader(TRIGGERED_BY_HEADER, event.triggeredBy().toString());
            return message;
        };
    }
}
