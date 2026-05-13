package com.agentweave.knowledge.messaging.consumer;

import com.agentweave.knowledge.application.DocumentIndexingService;
import com.agentweave.knowledge.application.DocumentVectorIndexingResult;
import com.agentweave.knowledge.messaging.application.DocumentMessageIdempotencyService;
import com.agentweave.knowledge.messaging.event.DocumentProcessingEvent;
import com.agentweave.knowledge.messaging.publisher.DocumentVectorIndexedEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "agentweave.document-pipeline.rabbitmq", name = "enabled", havingValue = "true")
public class DocumentVectorIndexConsumer {

    public static final String CONSUMER_NAME = "document-vector-index-consumer";

    private static final Logger log = LoggerFactory.getLogger(DocumentVectorIndexConsumer.class);

    private final DocumentIndexingService documentIndexingService;
    private final DocumentMessageIdempotencyService idempotencyService;
    private final DocumentVectorIndexedEventPublisher documentVectorIndexedEventPublisher;

    public DocumentVectorIndexConsumer(
            DocumentIndexingService documentIndexingService,
            DocumentMessageIdempotencyService idempotencyService,
            DocumentVectorIndexedEventPublisher documentVectorIndexedEventPublisher) {
        this.documentIndexingService = documentIndexingService;
        this.idempotencyService = idempotencyService;
        this.documentVectorIndexedEventPublisher = documentVectorIndexedEventPublisher;
    }

    @RabbitListener(queues = "${agentweave.document-pipeline.rabbitmq.queue-prefix}.vector-index.queue")
    public void handle(DocumentProcessingEvent event) {
        if (idempotencyService.isProcessed(event)) {
            log.info(
                    "Document vector index event already processed: eventId={}, documentId={}, traceId={}",
                    event.eventId(),
                    event.documentId(),
                    event.traceId());
            return;
        }

        log.info(
                "Document vector index event received: eventId={}, documentId={}, traceId={}, triggeredBy={}",
                event.eventId(),
                event.documentId(),
                event.traceId(),
                event.triggeredBy());
        DocumentVectorIndexingResult result = documentIndexingService.indexChunkedDocument(
                event.documentId(),
                event.traceId());
        documentVectorIndexedEventPublisher.publish(result.document(), event.traceId());
        idempotencyService.markProcessed(event, CONSUMER_NAME);
    }
}
