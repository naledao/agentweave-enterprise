package com.agentweave.knowledge.messaging.consumer;

import com.agentweave.knowledge.application.DocumentChunkingResult;
import com.agentweave.knowledge.application.DocumentReindexPipelineService;
import com.agentweave.knowledge.messaging.application.DocumentMessageFailureService;
import com.agentweave.knowledge.messaging.application.DocumentMessageIdempotencyService;
import com.agentweave.knowledge.messaging.event.DocumentProcessingEvent;
import com.agentweave.knowledge.messaging.publisher.DocumentChunkedEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "agentweave.document-pipeline.rabbitmq", name = "enabled", havingValue = "true")
public class DocumentReindexConsumer {

    public static final String CONSUMER_NAME = "document-reindex-consumer";

    private static final Logger log = LoggerFactory.getLogger(DocumentReindexConsumer.class);

    private final DocumentReindexPipelineService documentReindexPipelineService;
    private final DocumentMessageIdempotencyService idempotencyService;
    private final DocumentMessageFailureService failureService;
    private final DocumentChunkedEventPublisher documentChunkedEventPublisher;

    public DocumentReindexConsumer(
            DocumentReindexPipelineService documentReindexPipelineService,
            DocumentMessageIdempotencyService idempotencyService,
            DocumentMessageFailureService failureService,
            DocumentChunkedEventPublisher documentChunkedEventPublisher) {
        this.documentReindexPipelineService = documentReindexPipelineService;
        this.idempotencyService = idempotencyService;
        this.failureService = failureService;
        this.documentChunkedEventPublisher = documentChunkedEventPublisher;
    }

    @RabbitListener(queues = "${agentweave.document-pipeline.rabbitmq.queue-prefix}.reindex.queue")
    public void handle(DocumentProcessingEvent event) {
        if (idempotencyService.isProcessed(event)) {
            log.info(
                    "Document reindex event already processed: eventId={}, documentId={}, traceId={}",
                    event.eventId(),
                    event.documentId(),
                    event.traceId());
            return;
        }

        log.info(
                "Document reindex event received: eventId={}, documentId={}, traceId={}, triggeredBy={}",
                event.eventId(),
                event.documentId(),
                event.traceId(),
                event.triggeredBy());
        try {
            DocumentChunkingResult result = documentReindexPipelineService.prepareReindex(
                    event.documentId(),
                    event.traceId());
            documentChunkedEventPublisher.publish(
                    result.document(),
                    result.chunkCount(),
                    event.traceId(),
                    event.triggeredBy(),
                    true);
            idempotencyService.markProcessed(event, CONSUMER_NAME);
        } catch (RuntimeException ex) {
            throw failureService.handleConsumerFailure(event, CONSUMER_NAME, ex);
        }
    }
}
