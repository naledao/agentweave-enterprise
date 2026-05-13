package com.agentweave.knowledge.messaging.consumer;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agentweave.knowledge.application.DocumentChunkingResult;
import com.agentweave.knowledge.application.DocumentReindexPipelineService;
import com.agentweave.knowledge.domain.DocumentEntity;
import com.agentweave.knowledge.messaging.application.DocumentMessageFailureService;
import com.agentweave.knowledge.messaging.application.DocumentMessageIdempotencyService;
import com.agentweave.knowledge.messaging.event.DocumentProcessingEvent;
import com.agentweave.knowledge.messaging.event.DocumentProcessingEventType;
import com.agentweave.knowledge.messaging.publisher.DocumentChunkedEventPublisher;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DocumentReindexConsumerTest {

    private final DocumentReindexPipelineService documentReindexPipelineService =
            org.mockito.Mockito.mock(DocumentReindexPipelineService.class);
    private final DocumentMessageIdempotencyService idempotencyService =
            org.mockito.Mockito.mock(DocumentMessageIdempotencyService.class);
    private final DocumentMessageFailureService failureService =
            org.mockito.Mockito.mock(DocumentMessageFailureService.class);
    private final DocumentChunkedEventPublisher documentChunkedEventPublisher =
            org.mockito.Mockito.mock(DocumentChunkedEventPublisher.class);
    private final DocumentReindexConsumer consumer = new DocumentReindexConsumer(
            documentReindexPipelineService,
            idempotencyService,
            failureService,
            documentChunkedEventPublisher);

    private DocumentProcessingEvent event;
    private DocumentEntity document;

    @BeforeEach
    void setUp() {
        UUID documentId = UUID.randomUUID();
        UUID triggeredBy = UUID.randomUUID();
        event = DocumentProcessingEvent.create(
                DocumentProcessingEventType.DOCUMENT_REINDEX_REQUESTED,
                documentId,
                "trace-reindex",
                triggeredBy,
                Map.of("operation", "reindex"));
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
    void preparesReindexPublishesChunkedEventAndMarksProcessed() {
        DocumentChunkingResult result = new DocumentChunkingResult(document, 2);
        when(idempotencyService.isProcessed(event)).thenReturn(false);
        when(documentReindexPipelineService.prepareReindex(event.documentId(), event.traceId()))
                .thenReturn(result);

        consumer.handle(event);

        verify(documentReindexPipelineService).prepareReindex(event.documentId(), event.traceId());
        verify(documentChunkedEventPublisher)
                .publish(document, 2, event.traceId(), event.triggeredBy(), true);
        verify(idempotencyService).markProcessed(event, DocumentReindexConsumer.CONSUMER_NAME);
    }

    @Test
    void skipsDuplicateEvent() {
        when(idempotencyService.isProcessed(event)).thenReturn(true);

        consumer.handle(event);

        verify(documentReindexPipelineService, never()).prepareReindex(event.documentId(), event.traceId());
        verify(documentChunkedEventPublisher, never())
                .publish(document, 2, event.traceId(), event.triggeredBy(), true);
        verify(idempotencyService, never()).markProcessed(event, DocumentReindexConsumer.CONSUMER_NAME);
    }

    @Test
    void doesNotMarkProcessedWhenNextEventPublishFails() {
        DocumentChunkingResult result = new DocumentChunkingResult(document, 2);
        when(idempotencyService.isProcessed(event)).thenReturn(false);
        when(documentReindexPipelineService.prepareReindex(event.documentId(), event.traceId()))
                .thenReturn(result);
        IllegalStateException exception = new IllegalStateException("rabbitmq unavailable");
        doThrow(exception)
                .when(documentChunkedEventPublisher)
                .publish(document, 2, event.traceId(), event.triggeredBy(), true);
        when(failureService.handleConsumerFailure(event, DocumentReindexConsumer.CONSUMER_NAME, exception))
                .thenReturn(exception);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> consumer.handle(event))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("rabbitmq unavailable");

        verify(failureService).handleConsumerFailure(event, DocumentReindexConsumer.CONSUMER_NAME, exception);
        verify(idempotencyService, never()).markProcessed(event, DocumentReindexConsumer.CONSUMER_NAME);
    }
}
