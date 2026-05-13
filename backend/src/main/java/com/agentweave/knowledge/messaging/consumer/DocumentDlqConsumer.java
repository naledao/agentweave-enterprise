package com.agentweave.knowledge.messaging.consumer;

import com.agentweave.knowledge.messaging.application.DocumentMessageFailureService;
import com.agentweave.knowledge.messaging.event.DocumentProcessingEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "agentweave.document-pipeline.rabbitmq", name = "enabled", havingValue = "true")
public class DocumentDlqConsumer {

    public static final String CONSUMER_NAME = "document-dlq-consumer";

    private static final Logger log = LoggerFactory.getLogger(DocumentDlqConsumer.class);

    private final DocumentMessageFailureService failureService;

    public DocumentDlqConsumer(DocumentMessageFailureService failureService) {
        this.failureService = failureService;
    }

    @RabbitListener(queues = {
            "${agentweave.document-pipeline.rabbitmq.queue-prefix}.parse.dlq",
            "${agentweave.document-pipeline.rabbitmq.queue-prefix}.chunk.dlq",
            "${agentweave.document-pipeline.rabbitmq.queue-prefix}.vector-index.dlq",
            "${agentweave.document-pipeline.rabbitmq.queue-prefix}.graphrag-index.dlq",
            "${agentweave.document-pipeline.rabbitmq.queue-prefix}.reindex.dlq"
    })
    public void handle(
            DocumentProcessingEvent event,
            @Header(name = "amqp_receivedRoutingKey", required = false) String queue) {
        String dlq = queue == null || queue.isBlank() ? "unknown" : queue;
        log.info(
                "Document DLQ event received: eventId={}, eventType={}, documentId={}, traceId={}, queue={}",
                event.eventId(),
                event.eventType(),
                event.documentId(),
                event.traceId(),
                dlq);
        failureService.markDeadLettered(event, dlq, CONSUMER_NAME);
    }
}
