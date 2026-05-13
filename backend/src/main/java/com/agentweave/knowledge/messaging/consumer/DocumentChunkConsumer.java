package com.agentweave.knowledge.messaging.consumer;

import com.agentweave.knowledge.application.DocumentChunkPipelineService;
import com.agentweave.knowledge.application.DocumentChunkingResult;
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
public class DocumentChunkConsumer {

    public static final String CONSUMER_NAME = "document-chunk-consumer";

    private static final Logger log = LoggerFactory.getLogger(DocumentChunkConsumer.class);

    private final DocumentChunkPipelineService documentChunkPipelineService;
    private final DocumentMessageIdempotencyService idempotencyService;
    private final DocumentChunkedEventPublisher documentChunkedEventPublisher;

    public DocumentChunkConsumer(
            DocumentChunkPipelineService documentChunkPipelineService,
            DocumentMessageIdempotencyService idempotencyService,
            DocumentChunkedEventPublisher documentChunkedEventPublisher) {
        this.documentChunkPipelineService = documentChunkPipelineService;
        this.idempotencyService = idempotencyService;
        this.documentChunkedEventPublisher = documentChunkedEventPublisher;
    }

    @RabbitListener(queues = "${agentweave.document-pipeline.rabbitmq.queue-prefix}.chunk.queue")
    public void handle(DocumentProcessingEvent event) {
        if (idempotencyService.isProcessed(event)) {
            log.info(
                    "Document chunk event already processed: eventId={}, documentId={}, traceId={}",
                    event.eventId(),
                    event.documentId(),
                    event.traceId());
            return;
        }

        log.info(
                "Document chunk event received: eventId={}, documentId={}, traceId={}, triggeredBy={}",
                event.eventId(),
                event.documentId(),
                event.traceId(),
                event.triggeredBy());
        DocumentChunkingResult result = documentChunkPipelineService.chunkParsedDocument(
                event.documentId(),
                event.traceId());
        documentChunkedEventPublisher.publish(result.document(), result.chunkCount(), event.traceId());
        idempotencyService.markProcessed(event, CONSUMER_NAME);
    }
}
