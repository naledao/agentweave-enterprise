package com.agentweave.knowledge.messaging.consumer;

import com.agentweave.graphrag.application.GraphRagIndexingJob;
import com.agentweave.graphrag.dto.GraphRagIndexSummaryResponse;
import com.agentweave.knowledge.domain.DocumentEntity;
import com.agentweave.knowledge.domain.DocumentStatus;
import com.agentweave.knowledge.messaging.application.DocumentMessageFailureService;
import com.agentweave.knowledge.messaging.application.DocumentMessageIdempotencyService;
import com.agentweave.knowledge.messaging.event.DocumentProcessingEvent;
import com.agentweave.knowledge.repository.DocumentRepository;
import com.agentweave.shared.exception.BusinessException;
import com.agentweave.shared.exception.ErrorCode;
import com.agentweave.shared.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "agentweave.document-pipeline.rabbitmq", name = "enabled", havingValue = "true")
public class DocumentGraphRagIndexConsumer {

    public static final String CONSUMER_NAME = "document-graphrag-index-consumer";

    private static final Logger log = LoggerFactory.getLogger(DocumentGraphRagIndexConsumer.class);

    private final GraphRagIndexingJob graphRagIndexingJob;
    private final DocumentMessageIdempotencyService idempotencyService;
    private final DocumentMessageFailureService failureService;
    private final DocumentRepository documentRepository;

    public DocumentGraphRagIndexConsumer(
            GraphRagIndexingJob graphRagIndexingJob,
            DocumentMessageIdempotencyService idempotencyService,
            DocumentMessageFailureService failureService,
            DocumentRepository documentRepository) {
        this.graphRagIndexingJob = graphRagIndexingJob;
        this.idempotencyService = idempotencyService;
        this.failureService = failureService;
        this.documentRepository = documentRepository;
    }

    @RabbitListener(queues = "${agentweave.document-pipeline.rabbitmq.queue-prefix}.graphrag-index.queue")
    public void handle(DocumentProcessingEvent event) {
        if (idempotencyService.isProcessed(event)) {
            log.info(
                    "Document GraphRAG index event already processed: eventId={}, documentId={}, traceId={}",
                    event.eventId(),
                    event.documentId(),
                    event.traceId());
            return;
        }

        log.info(
                "Document GraphRAG index event received: eventId={}, documentId={}, traceId={}, triggeredBy={}",
                event.eventId(),
                event.documentId(),
                event.traceId(),
                event.triggeredBy());
        try {
            validateIndexed(event);
            GraphRagIndexSummaryResponse summary = graphRagIndexingJob.execute(event.documentId(), event.traceId());
            if (!"indexed".equals(summary.status())) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, failureMessage(summary));
            }
            idempotencyService.markProcessed(event, CONSUMER_NAME);
        } catch (RuntimeException ex) {
            throw failureService.handleConsumerFailure(event, CONSUMER_NAME, ex);
        }
    }

    private void validateIndexed(DocumentProcessingEvent event) {
        DocumentEntity document = documentRepository.findById(event.documentId())
                .orElseThrow(() -> new ResourceNotFoundException("document not found"));
        if (document.getStatus() != DocumentStatus.INDEXED) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_FAILED,
                    "document must be indexed before graph indexing");
        }
    }

    private String failureMessage(GraphRagIndexSummaryResponse summary) {
        if (summary.errorMessage() != null && !summary.errorMessage().isBlank()) {
            return "GraphRAG indexing failed: " + summary.errorMessage();
        }
        return "GraphRAG indexing did not complete";
    }
}
