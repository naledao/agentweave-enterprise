package com.agentweave.graphrag.application;

import com.agentweave.graphrag.domain.GraphRagRetrievalLog;
import com.agentweave.graphrag.repository.GraphRagRetrievalLogRepository;
import com.agentweave.graphrag.dto.GraphRagRetrievalResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class GraphRagRetrievalLogService {

    private final GraphRagRetrievalLogRepository graphRagRetrievalLogRepository;
    private final TransactionTemplate transactionTemplate;

    public GraphRagRetrievalLogService(
            GraphRagRetrievalLogRepository graphRagRetrievalLogRepository,
            TransactionTemplate transactionTemplate) {
        this.graphRagRetrievalLogRepository = graphRagRetrievalLogRepository;
        this.transactionTemplate = transactionTemplate;
    }

    public GraphRagRetrievalLog start(
            UUID conversationId,
            UUID messageId,
            String traceId,
            String query,
            String businessDomain,
            String permissionLevel,
            UUID documentId,
            int maxDepth,
            int maxPathCount,
            List<String> resolvedEntities) {
        return transactionTemplate.execute(status -> graphRagRetrievalLogRepository.saveAndFlush(
                new GraphRagRetrievalLog(
                        UUID.randomUUID(),
                        conversationId,
                        messageId,
                        traceId,
                        query,
                        "GRAPH_ONLY",
                        businessDomain,
                        permissionLevel,
                        documentId,
                        maxDepth,
                        maxPathCount,
                        resolvedEntities)));
    }

    public void markCompleted(GraphRagRetrievalLog log, GraphRagRetrievalResponse response) {
        transactionTemplate.executeWithoutResult(status -> {
            GraphRagRetrievalLog persisted = graphRagRetrievalLogRepository.findById(log.getId()).orElseThrow();
            persisted.markCompleted(
                    response.matchedPathCount(),
                    response.filteredPathCount(),
                    response.resolvedEntities(),
                    response.sourceChunkIds(),
                    response.confidenceSummary());
            graphRagRetrievalLogRepository.saveAndFlush(persisted);
        });
    }

    public void markFailed(GraphRagRetrievalLog log, String errorMessage) {
        transactionTemplate.executeWithoutResult(status -> {
            GraphRagRetrievalLog persisted = graphRagRetrievalLogRepository.findById(log.getId()).orElseThrow();
            persisted.markFailed(errorMessage);
            graphRagRetrievalLogRepository.saveAndFlush(persisted);
        });
    }
}
