package com.agentweave.graphrag.application;

import com.agentweave.graphrag.domain.GraphRagIndexLog;
import com.agentweave.graphrag.domain.GraphRagIndexStatus;
import com.agentweave.graphrag.dto.GraphRagIndexSummaryResponse;
import com.agentweave.graphrag.repository.GraphRagIndexLogRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class GraphRagIndexLogService {

    private final GraphRagIndexLogRepository graphRagIndexLogRepository;
    private final TransactionTemplate transactionTemplate;

    public GraphRagIndexLogService(
            GraphRagIndexLogRepository graphRagIndexLogRepository,
            TransactionTemplate transactionTemplate) {
        this.graphRagIndexLogRepository = graphRagIndexLogRepository;
        this.transactionTemplate = transactionTemplate;
    }

    public GraphRagIndexLog start(UUID documentId, String traceId, int chunkCount) {
        return transactionTemplate.execute(status -> graphRagIndexLogRepository.saveAndFlush(
                new GraphRagIndexLog(UUID.randomUUID(), documentId, traceId, chunkCount)));
    }

    public void markCompleted(GraphRagIndexLog log, int entityCount, int relationshipCount, int chunkCount) {
        transactionTemplate.executeWithoutResult(status -> {
            GraphRagIndexLog persisted = graphRagIndexLogRepository.findById(log.getId()).orElseThrow();
            persisted.markCompleted(entityCount, relationshipCount, chunkCount);
            graphRagIndexLogRepository.saveAndFlush(persisted);
        });
    }

    public void markFailed(GraphRagIndexLog log, String errorMessage, int entityCount, int relationshipCount, int chunkCount) {
        transactionTemplate.executeWithoutResult(status -> {
            GraphRagIndexLog persisted = graphRagIndexLogRepository.findById(log.getId()).orElseThrow();
            persisted.markFailed(errorMessage, entityCount, relationshipCount, chunkCount);
            graphRagIndexLogRepository.saveAndFlush(persisted);
        });
    }

    public GraphRagIndexSummaryResponse latestSummary(UUID documentId) {
        return graphRagIndexLogRepository.findFirstByDocumentIdOrderByCreatedAtDesc(documentId)
                .map(this::toSummary)
                .orElseGet(GraphRagIndexSummaryResponse::pending);
    }

    private GraphRagIndexSummaryResponse toSummary(GraphRagIndexLog log) {
        GraphRagIndexStatus status = log.getStatus();
        return switch (status) {
            case PROCESSING -> GraphRagIndexSummaryResponse.processing(log.getTraceId(), log.getChunkCount());
            case INDEXED -> GraphRagIndexSummaryResponse.indexed(
                    log.getTraceId(),
                    log.getEntityCount(),
                    log.getRelationshipCount(),
                    log.getChunkCount(),
                    completedAt(log.getCompletedAt()));
            case FAILED -> GraphRagIndexSummaryResponse.failed(
                    log.getTraceId(),
                    log.getEntityCount(),
                    log.getRelationshipCount(),
                    log.getChunkCount(),
                    log.getErrorMessage());
        };
    }

    private Instant completedAt(Instant completedAt) {
        return completedAt;
    }
}
