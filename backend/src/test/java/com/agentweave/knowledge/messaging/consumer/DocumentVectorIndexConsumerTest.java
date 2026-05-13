package com.agentweave.knowledge.messaging.consumer;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agentweave.knowledge.application.DocumentIndexingService;
import com.agentweave.knowledge.application.DocumentVectorIndexingResult;
import com.agentweave.knowledge.domain.DocumentEntity;
import com.agentweave.knowledge.messaging.application.DocumentMessageIdempotencyService;
import com.agentweave.knowledge.messaging.event.DocumentProcessingEvent;
import com.agentweave.knowledge.messaging.event.DocumentProcessingEventType;
import com.agentweave.knowledge.messaging.publisher.DocumentVectorIndexedEventPublisher;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DocumentVectorIndexConsumerTest {

    private final DocumentIndexingService documentIndexingService =
            org.mockito.Mockito.mock(DocumentIndexingService.class);
    private final DocumentMessageIdempotencyService idempotencyService =
            org.mockito.Mockito.mock(DocumentMessageIdempotencyService.class);
    private final DocumentVectorIndexedEventPublisher documentVectorIndexedEventPublisher =
            org.mockito.Mockito.mock(DocumentVectorIndexedEventPublisher.class);
    private final DocumentVectorIndexConsumer consumer = new DocumentVectorIndexConsumer(
            documentIndexingService,
            idempotencyService,
            documentVectorIndexedEventPublisher);

    private DocumentProcessingEvent event;
    private DocumentEntity document;

    @BeforeEach
    void setUp() {
        UUID documentId = UUID.randomUUID();
        UUID triggeredBy = UUID.randomUUID();
        event = DocumentProcessingEvent.create(
                DocumentProcessingEventType.DOCUMENT_CHUNKED,
                documentId,
                "trace-vector-index",
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
    void indexesDocumentPublishesVectorIndexedEventAndMarksProcessed() {
        DocumentVectorIndexingResult result = new DocumentVectorIndexingResult(document, 2);
        when(idempotencyService.isProcessed(event)).thenReturn(false);
        when(documentIndexingService.indexChunkedDocument(event.documentId(), event.traceId()))
                .thenReturn(result);

        consumer.handle(event);

        verify(documentIndexingService).indexChunkedDocument(event.documentId(), event.traceId());
        verify(documentVectorIndexedEventPublisher).publish(document, event.traceId());
        verify(idempotencyService).markProcessed(event, DocumentVectorIndexConsumer.CONSUMER_NAME);
    }

    @Test
    void skipsDuplicateEvent() {
        when(idempotencyService.isProcessed(event)).thenReturn(true);

        consumer.handle(event);

        verify(documentIndexingService, never()).indexChunkedDocument(event.documentId(), event.traceId());
        verify(documentVectorIndexedEventPublisher, never()).publish(document, event.traceId());
        verify(idempotencyService, never()).markProcessed(event, DocumentVectorIndexConsumer.CONSUMER_NAME);
    }

    @Test
    void doesNotMarkProcessedWhenNextEventPublishFails() {
        DocumentVectorIndexingResult result = new DocumentVectorIndexingResult(document, 2);
        when(idempotencyService.isProcessed(event)).thenReturn(false);
        when(documentIndexingService.indexChunkedDocument(event.documentId(), event.traceId()))
                .thenReturn(result);
        doThrow(new IllegalStateException("rabbitmq unavailable"))
                .when(documentVectorIndexedEventPublisher)
                .publish(document, event.traceId());

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> consumer.handle(event))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("rabbitmq unavailable");

        verify(idempotencyService, never()).markProcessed(event, DocumentVectorIndexConsumer.CONSUMER_NAME);
    }
}
