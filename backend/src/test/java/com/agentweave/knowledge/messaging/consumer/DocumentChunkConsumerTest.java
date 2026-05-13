package com.agentweave.knowledge.messaging.consumer;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agentweave.knowledge.application.DocumentChunkPipelineService;
import com.agentweave.knowledge.application.DocumentChunkingResult;
import com.agentweave.knowledge.domain.DocumentEntity;
import com.agentweave.knowledge.messaging.application.DocumentMessageIdempotencyService;
import com.agentweave.knowledge.messaging.event.DocumentProcessingEvent;
import com.agentweave.knowledge.messaging.event.DocumentProcessingEventType;
import com.agentweave.knowledge.messaging.publisher.DocumentChunkedEventPublisher;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DocumentChunkConsumerTest {

    private final DocumentChunkPipelineService documentChunkPipelineService =
            org.mockito.Mockito.mock(DocumentChunkPipelineService.class);
    private final DocumentMessageIdempotencyService idempotencyService =
            org.mockito.Mockito.mock(DocumentMessageIdempotencyService.class);
    private final DocumentChunkedEventPublisher documentChunkedEventPublisher =
            org.mockito.Mockito.mock(DocumentChunkedEventPublisher.class);
    private final DocumentChunkConsumer consumer = new DocumentChunkConsumer(
            documentChunkPipelineService,
            idempotencyService,
            documentChunkedEventPublisher);

    private DocumentProcessingEvent event;
    private DocumentEntity document;

    @BeforeEach
    void setUp() {
        UUID documentId = UUID.randomUUID();
        UUID triggeredBy = UUID.randomUUID();
        event = DocumentProcessingEvent.create(
                DocumentProcessingEventType.DOCUMENT_PARSED,
                documentId,
                "trace-chunk",
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
    void chunksDocumentPublishesChunkedEventAndMarksProcessed() {
        DocumentChunkingResult result = new DocumentChunkingResult(document, 2);
        when(idempotencyService.isProcessed(event)).thenReturn(false);
        when(documentChunkPipelineService.chunkParsedDocument(event.documentId(), event.traceId()))
                .thenReturn(result);

        consumer.handle(event);

        verify(documentChunkPipelineService).chunkParsedDocument(event.documentId(), event.traceId());
        verify(documentChunkedEventPublisher).publish(document, 2, event.traceId());
        verify(idempotencyService).markProcessed(event, DocumentChunkConsumer.CONSUMER_NAME);
    }

    @Test
    void skipsDuplicateEvent() {
        when(idempotencyService.isProcessed(event)).thenReturn(true);

        consumer.handle(event);

        verify(documentChunkPipelineService, never()).chunkParsedDocument(event.documentId(), event.traceId());
        verify(documentChunkedEventPublisher, never()).publish(document, 2, event.traceId());
        verify(idempotencyService, never()).markProcessed(event, DocumentChunkConsumer.CONSUMER_NAME);
    }

    @Test
    void doesNotMarkProcessedWhenNextEventPublishFails() {
        DocumentChunkingResult result = new DocumentChunkingResult(document, 2);
        when(idempotencyService.isProcessed(event)).thenReturn(false);
        when(documentChunkPipelineService.chunkParsedDocument(event.documentId(), event.traceId()))
                .thenReturn(result);
        doThrow(new IllegalStateException("rabbitmq unavailable"))
                .when(documentChunkedEventPublisher)
                .publish(document, 2, event.traceId());

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> consumer.handle(event))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("rabbitmq unavailable");

        verify(idempotencyService, never()).markProcessed(event, DocumentChunkConsumer.CONSUMER_NAME);
    }
}
