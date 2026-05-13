package com.agentweave.knowledge.application;

import com.agentweave.graphrag.application.GraphRagIndexingJob;
import com.agentweave.knowledge.messaging.publisher.DocumentVectorIndexedEventPublisher;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public class GraphRagIndexingScheduler {

    private static final Logger log = LoggerFactory.getLogger(GraphRagIndexingScheduler.class);

    private final GraphRagIndexingJob graphRagIndexingJob;
    private final ObjectProvider<DocumentVectorIndexedEventPublisher> vectorIndexedEventPublisherProvider;

    public GraphRagIndexingScheduler(
            GraphRagIndexingJob graphRagIndexingJob,
            ObjectProvider<DocumentVectorIndexedEventPublisher> vectorIndexedEventPublisherProvider) {
        this.graphRagIndexingJob = graphRagIndexingJob;
        this.vectorIndexedEventPublisherProvider = vectorIndexedEventPublisherProvider;
    }

    public void enqueue(UUID documentId, String traceId) {
        DocumentVectorIndexedEventPublisher publisher = vectorIndexedEventPublisherProvider.getIfAvailable();
        if (publisher != null) {
            log.info("GraphRAG indexing event queued: documentId={}, traceId={}", documentId, traceId);
            publisher.publish(documentId, traceId);
            return;
        }
        log.info("GraphRAG indexing queued locally: documentId={}, traceId={}", documentId, traceId);
        graphRagIndexingJob.execute(documentId, traceId);
    }
}
