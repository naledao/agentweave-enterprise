package com.agentweave.knowledge.messaging.consumer;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agentweave.knowledge.application.DocumentApplicationService;
import com.agentweave.knowledge.domain.DocumentEntity;
import com.agentweave.knowledge.messaging.application.DocumentMessageFailureService;
import com.agentweave.knowledge.messaging.application.DocumentMessageIdempotencyService;
import com.agentweave.knowledge.messaging.event.DocumentProcessingEvent;
import com.agentweave.knowledge.messaging.event.DocumentProcessingEventType;
import com.agentweave.knowledge.messaging.publisher.DocumentParsedEventPublisher;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DocumentParseConsumerTest {

    private final DocumentApplicationService documentApplicationService =
            org.mockito.Mockito.mock(DocumentApplicationService.class);
    private final DocumentMessageIdempotencyService idempotencyService =
            org.mockito.Mockito.mock(DocumentMessageIdempotencyService.class);
    private final DocumentMessageFailureService failureService =
            org.mockito.Mockito.mock(DocumentMessageFailureService.class);
    private final DocumentParsedEventPublisher documentParsedEventPublisher =
            org.mockito.Mockito.mock(DocumentParsedEventPublisher.class);
    private final DocumentParseConsumer consumer = new DocumentParseConsumer(
            documentApplicationService,
            idempotencyService,
            failureService,
            documentParsedEventPublisher);

    private DocumentProcessingEvent event;
    private DocumentEntity document;

    @BeforeEach
    void setUp() {
        UUID documentId = UUID.randomUUID();
        UUID triggeredBy = UUID.randomUUID();
        event = DocumentProcessingEvent.create(
                DocumentProcessingEventType.DOCUMENT_UPLOADED,
                documentId,
                "trace-parse",
                triggeredBy,
                Map.of("source", "runbook"));
        document = new DocumentEntity(
                documentId,
                "runbook.txt",
                "text/plain",
                12,
                "agentweave-documents",
                "documents/runbook.txt",
                "checksum",
                triggeredBy,
                "runbook",
                "ops",
                "faq",
                "internal",
                null,
                null,
                "");
    }

    @Test
    void parsesDocumentPublishesParsedEventAndMarksProcessed() {
        when(idempotencyService.isProcessed(event)).thenReturn(false);
        when(documentApplicationService.parseUploadedDocument(event.documentId(), event.traceId()))
                .thenReturn(document);

        consumer.handle(event);

        verify(documentApplicationService).parseUploadedDocument(event.documentId(), event.traceId());
        verify(documentParsedEventPublisher).publish(document, event.traceId());
        verify(idempotencyService).markProcessed(event, DocumentParseConsumer.CONSUMER_NAME);
    }

    @Test
    void skipsDuplicateEvent() {
        when(idempotencyService.isProcessed(event)).thenReturn(true);

        consumer.handle(event);

        verify(documentApplicationService, never()).parseUploadedDocument(event.documentId(), event.traceId());
        verify(documentParsedEventPublisher, never()).publish(document, event.traceId());
        verify(idempotencyService, never()).markProcessed(event, DocumentParseConsumer.CONSUMER_NAME);
    }

    @Test
    void doesNotMarkProcessedWhenNextEventPublishFails() {
        when(idempotencyService.isProcessed(event)).thenReturn(false);
        when(documentApplicationService.parseUploadedDocument(event.documentId(), event.traceId()))
                .thenReturn(document);
        IllegalStateException exception = new IllegalStateException("rabbitmq unavailable");
        doThrow(exception)
                .when(documentParsedEventPublisher)
                .publish(document, event.traceId());
        when(failureService.handleConsumerFailure(event, DocumentParseConsumer.CONSUMER_NAME, exception))
                .thenReturn(exception);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> consumer.handle(event))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("rabbitmq unavailable");

        verify(failureService).handleConsumerFailure(event, DocumentParseConsumer.CONSUMER_NAME, exception);
        verify(idempotencyService, never()).markProcessed(event, DocumentParseConsumer.CONSUMER_NAME);
    }
}
