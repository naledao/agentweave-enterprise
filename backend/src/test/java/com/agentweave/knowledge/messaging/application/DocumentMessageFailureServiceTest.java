package com.agentweave.knowledge.messaging.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.agentweave.knowledge.application.DocumentStatusService;
import com.agentweave.knowledge.messaging.event.DocumentProcessingEvent;
import com.agentweave.knowledge.messaging.event.DocumentProcessingEventType;
import com.agentweave.shared.exception.BusinessException;
import com.agentweave.shared.exception.ErrorCode;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;

class DocumentMessageFailureServiceTest {

    private final DocumentStatusService documentStatusService =
            org.mockito.Mockito.mock(DocumentStatusService.class);
    private final DocumentMessageFailureService failureService =
            new DocumentMessageFailureService(documentStatusService);

    private DocumentProcessingEvent event;

    @BeforeEach
    void setUp() {
        event = DocumentProcessingEvent.create(
                DocumentProcessingEventType.DOCUMENT_UPLOADED,
                UUID.randomUUID(),
                "trace-failure",
                UUID.randomUUID(),
                Map.of("source", "runbook"));
    }

    @Test
    void nonRetryableBusinessExceptionMarksFailedAndRejectsWithoutRequeue() {
        BusinessException exception = new BusinessException(
                ErrorCode.VALIDATION_FAILED,
                "cleaned text must not be blank");

        RuntimeException result = failureService.handleConsumerFailure(
                event,
                "document-parse-consumer",
                exception);

        assertThat(result)
                .isInstanceOf(AmqpRejectAndDontRequeueException.class)
                .hasMessage("cleaned text must not be blank")
                .hasCause(exception);
        verify(documentStatusService)
                .markFailed(event.documentId(), "cleaned text must not be blank", event.traceId());
    }

    @Test
    void retryableExceptionKeepsOriginalExceptionForListenerRetry() {
        IllegalStateException exception = new IllegalStateException("database unavailable");

        RuntimeException result = failureService.handleConsumerFailure(
                event,
                "document-vector-index-consumer",
                exception);

        assertThat(result).isSameAs(exception);
        verify(documentStatusService, never())
                .markFailed(event.documentId(), "database unavailable", event.traceId());
    }

    @Test
    void deadLetteredMessageMarksDocumentFailedWithDlqSummary() {
        failureService.markDeadLettered(
                event,
                "agentweave.document.parse.dlq",
                "document-dlq-consumer");

        verify(documentStatusService).markFailed(
                event.documentId(),
                "document processing message moved to DLQ: queue=agentweave.document.parse.dlq, eventType=document.uploaded",
                event.traceId());
    }
}
