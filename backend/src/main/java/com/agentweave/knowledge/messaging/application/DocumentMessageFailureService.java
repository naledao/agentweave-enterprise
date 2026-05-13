package com.agentweave.knowledge.messaging.application;

import com.agentweave.knowledge.application.DocumentStatusService;
import com.agentweave.knowledge.messaging.event.DocumentProcessingEvent;
import com.agentweave.shared.exception.BusinessException;
import com.agentweave.shared.exception.ErrorCode;
import com.agentweave.shared.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.stereotype.Service;

@Service
public class DocumentMessageFailureService {

    private static final Logger log = LoggerFactory.getLogger(DocumentMessageFailureService.class);

    private final DocumentStatusService documentStatusService;

    public DocumentMessageFailureService(DocumentStatusService documentStatusService) {
        this.documentStatusService = documentStatusService;
    }

    public RuntimeException handleConsumerFailure(
            DocumentProcessingEvent event,
            String consumer,
            RuntimeException exception) {
        String summary = errorSummary(exception);
        if (isNonRetryable(exception)) {
            markFailed(event, summary);
            log.warn(
                    "Document message failed with non-retryable error: eventId={}, eventType={}, documentId={}, traceId={}, consumer={}, error={}",
                    event.eventId(),
                    event.eventType(),
                    event.documentId(),
                    event.traceId(),
                    consumer,
                    summary,
                    exception);
            return new AmqpRejectAndDontRequeueException(summary, exception);
        }
        log.warn(
                "Document message failed with retryable error: eventId={}, eventType={}, documentId={}, traceId={}, consumer={}, error={}",
                event.eventId(),
                event.eventType(),
                event.documentId(),
                event.traceId(),
                consumer,
                summary,
                exception);
        return exception;
    }

    public void markDeadLettered(DocumentProcessingEvent event, String queue, String consumer) {
        String summary = "document processing message moved to DLQ: queue=%s, eventType=%s"
                .formatted(queue, event.eventType());
        markFailed(event, summary);
        log.warn(
                "Document message dead-lettered: eventId={}, eventType={}, documentId={}, traceId={}, queue={}, consumer={}",
                event.eventId(),
                event.eventType(),
                event.documentId(),
                event.traceId(),
                queue,
                consumer);
    }

    public boolean isNonRetryable(RuntimeException exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof ResourceNotFoundException) {
                return true;
            }
            if (current instanceof BusinessException businessException
                    && (businessException.getErrorCode() == ErrorCode.VALIDATION_FAILED
                    || businessException.getErrorCode() == ErrorCode.RESOURCE_NOT_FOUND
                    || businessException.getErrorCode() == ErrorCode.ACCESS_DENIED)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private void markFailed(DocumentProcessingEvent event, String summary) {
        documentStatusService.markFailed(event.documentId(), summary, event.traceId());
    }

    private String errorSummary(Exception ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            message = ex.getClass().getSimpleName();
        }
        return message.length() > 1000 ? message.substring(0, 1000) : message;
    }
}
