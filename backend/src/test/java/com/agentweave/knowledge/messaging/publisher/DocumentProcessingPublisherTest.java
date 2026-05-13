package com.agentweave.knowledge.messaging.publisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.agentweave.knowledge.messaging.config.RabbitMqDocumentProperties;
import com.agentweave.knowledge.messaging.event.DocumentProcessingEvent;
import com.agentweave.knowledge.messaging.event.DocumentProcessingEventType;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

class DocumentProcessingPublisherTest {

    private final RabbitTemplate rabbitTemplate = org.mockito.Mockito.mock(RabbitTemplate.class);
    private final RabbitMqDocumentProperties properties = new RabbitMqDocumentProperties(
            true,
            "agentweave.document.exchange",
            "agentweave.document.retry.exchange",
            "agentweave.document.dlx",
            "agentweave.document");
    private final DocumentProcessingPublisher publisher = new DocumentProcessingPublisher(rabbitTemplate, properties);

    @Test
    void publishUsesEventTypeAsRoutingKeyAndWritesHeaders() {
        UUID eventId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        UUID triggeredBy = UUID.randomUUID();
        DocumentProcessingEvent event = new DocumentProcessingEvent(
                eventId,
                DocumentProcessingEventType.DOCUMENT_UPLOADED.routingKey(),
                1,
                documentId,
                "trace-upload",
                triggeredBy,
                Instant.now(),
                Map.of("source", "runbook"));

        publisher.publish(event);

        ArgumentCaptor<MessagePostProcessor> postProcessorCaptor =
                ArgumentCaptor.forClass(MessagePostProcessor.class);
        verify(rabbitTemplate).convertAndSend(
                eq("agentweave.document.exchange"),
                eq("document.uploaded"),
                eq(event),
                postProcessorCaptor.capture());

        Message message = new Message(new byte[0]);
        postProcessorCaptor.getValue().postProcessMessage(message);

        assertThat(header(message, DocumentProcessingPublisher.EVENT_ID_HEADER))
                .isEqualTo(eventId.toString());
        assertThat(header(message, DocumentProcessingPublisher.EVENT_TYPE_HEADER))
                .isEqualTo("document.uploaded");
        assertThat(header(message, DocumentProcessingPublisher.TRACE_ID_HEADER))
                .isEqualTo("trace-upload");
        assertThat(header(message, DocumentProcessingPublisher.DOCUMENT_ID_HEADER))
                .isEqualTo(documentId.toString());
        assertThat(header(message, DocumentProcessingPublisher.TRIGGERED_BY_HEADER))
                .isEqualTo(triggeredBy.toString());
    }

    private String header(Message message, String name) {
        return message.getMessageProperties().getHeaders().get(name).toString();
    }
}
