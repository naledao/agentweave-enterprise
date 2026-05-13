package com.agentweave.knowledge.messaging.consumer;

import static org.mockito.Mockito.verify;

import com.agentweave.knowledge.messaging.application.DocumentMessageFailureService;
import com.agentweave.knowledge.messaging.event.DocumentProcessingEvent;
import com.agentweave.knowledge.messaging.event.DocumentProcessingEventType;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DocumentDlqConsumerTest {

    private final DocumentMessageFailureService failureService =
            org.mockito.Mockito.mock(DocumentMessageFailureService.class);
    private final DocumentDlqConsumer consumer = new DocumentDlqConsumer(failureService);

    private DocumentProcessingEvent event;

    @BeforeEach
    void setUp() {
        event = DocumentProcessingEvent.create(
                DocumentProcessingEventType.DOCUMENT_UPLOADED,
                UUID.randomUUID(),
                "trace-dlq",
                UUID.randomUUID(),
                Map.of("source", "runbook"));
    }

    @Test
    void marksDocumentFailedWhenMessageArrivesInDlq() {
        consumer.handle(event, "agentweave.document.parse.dlq");

        verify(failureService).markDeadLettered(
                event,
                "agentweave.document.parse.dlq",
                DocumentDlqConsumer.CONSUMER_NAME);
    }

    @Test
    void usesUnknownQueueWhenDlqHeaderIsMissing() {
        consumer.handle(event, null);

        verify(failureService).markDeadLettered(
                event,
                "unknown",
                DocumentDlqConsumer.CONSUMER_NAME);
    }
}
