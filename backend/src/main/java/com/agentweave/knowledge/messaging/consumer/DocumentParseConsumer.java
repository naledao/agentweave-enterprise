package com.agentweave.knowledge.messaging.consumer;

import com.agentweave.knowledge.application.DocumentApplicationService;
import com.agentweave.knowledge.domain.DocumentEntity;
import com.agentweave.knowledge.messaging.application.DocumentMessageIdempotencyService;
import com.agentweave.knowledge.messaging.event.DocumentProcessingEvent;
import com.agentweave.knowledge.messaging.publisher.DocumentParsedEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "agentweave.document-pipeline.rabbitmq", name = "enabled", havingValue = "true")
public class DocumentParseConsumer {

    public static final String CONSUMER_NAME = "document-parse-consumer";

    private static final Logger log = LoggerFactory.getLogger(DocumentParseConsumer.class);

    private final DocumentApplicationService documentApplicationService;
    private final DocumentMessageIdempotencyService idempotencyService;
    private final DocumentParsedEventPublisher documentParsedEventPublisher;

    public DocumentParseConsumer(
            DocumentApplicationService documentApplicationService,
            DocumentMessageIdempotencyService idempotencyService,
            DocumentParsedEventPublisher documentParsedEventPublisher) {
        this.documentApplicationService = documentApplicationService;
        this.idempotencyService = idempotencyService;
        this.documentParsedEventPublisher = documentParsedEventPublisher;
    }

    @RabbitListener(queues = "${agentweave.document-pipeline.rabbitmq.queue-prefix}.parse.queue")
    public void handle(DocumentProcessingEvent event) {
        if (idempotencyService.isProcessed(event)) {
            log.info(
                    "Document parse event already processed: eventId={}, documentId={}, traceId={}",
                    event.eventId(),
                    event.documentId(),
                    event.traceId());
            return;
        }

        log.info(
                "Document parse event received: eventId={}, documentId={}, traceId={}, triggeredBy={}",
                event.eventId(),
                event.documentId(),
                event.traceId(),
                event.triggeredBy());
        DocumentEntity cleanedDocument = documentApplicationService.parseUploadedDocument(
                event.documentId(),
                event.traceId());
        documentParsedEventPublisher.publish(cleanedDocument, event.traceId());
        idempotencyService.markProcessed(event, CONSUMER_NAME);
    }
}
