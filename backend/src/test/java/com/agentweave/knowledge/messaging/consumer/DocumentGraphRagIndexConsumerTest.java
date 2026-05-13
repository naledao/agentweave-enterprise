package com.agentweave.knowledge.messaging.consumer;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import com.agentweave.graphrag.application.GraphRagIndexingJob;
import com.agentweave.graphrag.dto.GraphRagIndexSummaryResponse;
import com.agentweave.knowledge.domain.DocumentEntity;
import com.agentweave.knowledge.messaging.application.DocumentMessageFailureService;
import com.agentweave.knowledge.messaging.application.DocumentMessageIdempotencyService;
import com.agentweave.knowledge.messaging.event.DocumentProcessingEvent;
import com.agentweave.knowledge.messaging.event.DocumentProcessingEventType;
import com.agentweave.knowledge.repository.DocumentRepository;
import com.agentweave.shared.exception.BusinessException;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DocumentGraphRagIndexConsumerTest {

    private final GraphRagIndexingJob graphRagIndexingJob =
            org.mockito.Mockito.mock(GraphRagIndexingJob.class);
    private final DocumentMessageIdempotencyService idempotencyService =
            org.mockito.Mockito.mock(DocumentMessageIdempotencyService.class);
    private final DocumentMessageFailureService failureService =
            org.mockito.Mockito.mock(DocumentMessageFailureService.class);
    private final DocumentRepository documentRepository =
            org.mockito.Mockito.mock(DocumentRepository.class);
    private final DocumentGraphRagIndexConsumer consumer = new DocumentGraphRagIndexConsumer(
            graphRagIndexingJob,
            idempotencyService,
            failureService,
            documentRepository);

    private DocumentProcessingEvent event;
    private DocumentEntity document;

    @BeforeEach
    void setUp() {
        UUID documentId = UUID.randomUUID();
        UUID triggeredBy = UUID.randomUUID();
        event = DocumentProcessingEvent.create(
                DocumentProcessingEventType.DOCUMENT_VECTOR_INDEXED,
                documentId,
                "trace-graphrag-index",
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
        document.markIndexed(event.traceId());
    }

    @Test
    void runsGraphRagJobAndMarksProcessed() {
        GraphRagIndexSummaryResponse summary = GraphRagIndexSummaryResponse.indexed(
                event.traceId(),
                2,
                1,
                3,
                Instant.now());
        when(idempotencyService.isProcessed(event)).thenReturn(false);
        when(documentRepository.findById(event.documentId())).thenReturn(Optional.of(document));
        when(graphRagIndexingJob.execute(event.documentId(), event.traceId())).thenReturn(summary);

        consumer.handle(event);

        verify(graphRagIndexingJob).execute(event.documentId(), event.traceId());
        verify(idempotencyService).markProcessed(event, DocumentGraphRagIndexConsumer.CONSUMER_NAME);
    }

    @Test
    void skipsDuplicateEvent() {
        when(idempotencyService.isProcessed(event)).thenReturn(true);

        consumer.handle(event);

        verify(documentRepository, never()).findById(event.documentId());
        verify(graphRagIndexingJob, never()).execute(event.documentId(), event.traceId());
        verify(idempotencyService, never()).markProcessed(event, DocumentGraphRagIndexConsumer.CONSUMER_NAME);
    }

    @Test
    void doesNotMarkProcessedWhenGraphRagJobFails() {
        GraphRagIndexSummaryResponse summary = GraphRagIndexSummaryResponse.failed(
                event.traceId(),
                0,
                0,
                3,
                "llm unavailable");
        when(idempotencyService.isProcessed(event)).thenReturn(false);
        when(documentRepository.findById(event.documentId())).thenReturn(Optional.of(document));
        when(graphRagIndexingJob.execute(event.documentId(), event.traceId())).thenReturn(summary);
        when(failureService.handleConsumerFailure(
                eq(event),
                eq(DocumentGraphRagIndexConsumer.CONSUMER_NAME),
                any(RuntimeException.class)))
                .thenAnswer(invocation -> invocation.getArgument(2));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> consumer.handle(event))
                .isInstanceOf(BusinessException.class)
                .hasMessage("GraphRAG indexing failed: llm unavailable");

        verify(failureService).handleConsumerFailure(
                eq(event),
                eq(DocumentGraphRagIndexConsumer.CONSUMER_NAME),
                any(RuntimeException.class));
        verify(idempotencyService, never()).markProcessed(event, DocumentGraphRagIndexConsumer.CONSUMER_NAME);
    }

    @Test
    void doesNotRunJobWhenDocumentIsNotIndexed() {
        document.markEmbedding(event.traceId());
        when(idempotencyService.isProcessed(event)).thenReturn(false);
        when(documentRepository.findById(event.documentId())).thenReturn(Optional.of(document));
        when(failureService.handleConsumerFailure(
                eq(event),
                eq(DocumentGraphRagIndexConsumer.CONSUMER_NAME),
                any(RuntimeException.class)))
                .thenAnswer(invocation -> invocation.getArgument(2));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> consumer.handle(event))
                .isInstanceOf(BusinessException.class)
                .hasMessage("document must be indexed before graph indexing");

        verify(graphRagIndexingJob, never()).execute(event.documentId(), event.traceId());
        verify(failureService).handleConsumerFailure(
                eq(event),
                eq(DocumentGraphRagIndexConsumer.CONSUMER_NAME),
                any(RuntimeException.class));
        verify(idempotencyService, never()).markProcessed(event, DocumentGraphRagIndexConsumer.CONSUMER_NAME);
    }
}
