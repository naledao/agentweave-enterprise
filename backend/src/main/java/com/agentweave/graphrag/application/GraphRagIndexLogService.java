package com.agentweave.graphrag.application;

import com.agentweave.graphrag.domain.GraphRagIndexLog;
import com.agentweave.graphrag.domain.GraphRagIndexStatus;
import com.agentweave.graphrag.dto.GraphRagIndexSummaryResponse;
import com.agentweave.graphrag.infrastructure.GraphRagNeo4jProperties;
import com.agentweave.graphrag.repository.GraphRagIndexLogRepository;
import com.agentweave.observability.application.AgentWeaveMetrics;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class GraphRagIndexLogService {

    private final GraphRagIndexLogRepository graphRagIndexLogRepository;
    private final TransactionTemplate transactionTemplate;
    private final AgentWeaveMetrics agentWeaveMetrics;
    private final GraphRagNeo4jProperties graphRagNeo4jProperties;

    public GraphRagIndexLogService(
            GraphRagIndexLogRepository graphRagIndexLogRepository,
            TransactionTemplate transactionTemplate,
            AgentWeaveMetrics agentWeaveMetrics,
            GraphRagNeo4jProperties graphRagNeo4jProperties) {
        this.graphRagIndexLogRepository = graphRagIndexLogRepository;
        this.transactionTemplate = transactionTemplate;
        this.agentWeaveMetrics = agentWeaveMetrics;
        this.graphRagNeo4jProperties = graphRagNeo4jProperties;
    }

    public GraphRagIndexLog start(UUID documentId, String traceId, int chunkCount) {
        return transactionTemplate.execute(status -> graphRagIndexLogRepository.saveAndFlush(
                new GraphRagIndexLog(
                        UUID.randomUUID(),
                        documentId,
                        traceId,
                        chunkCount,
                        graphRagNeo4jProperties.enabled())));
    }

    public void markCompleted(
            GraphRagIndexLog log,
            String businessDomain,
            String permissionLevel,
            int entityCount,
            int relationshipCount,
            int chunkCount,
            int chunkEntityCount) {
        transactionTemplate.executeWithoutResult(status -> {
            GraphRagIndexLog persisted = graphRagIndexLogRepository.findById(log.getId()).orElseThrow();
            persisted.markCompleted(entityCount, relationshipCount, chunkCount, chunkEntityCount);
            graphRagIndexLogRepository.saveAndFlush(persisted);
            agentWeaveMetrics.recordGraphRagIndex(
                    businessDomain,
                    permissionLevel,
                    persisted.getStatus().name(),
                    persisted.isNeo4jEnabled(),
                    persisted.getDurationMs());
        });
    }

    public void markFailed(
            GraphRagIndexLog log,
            String businessDomain,
            String permissionLevel,
            String errorMessage,
            int entityCount,
            int relationshipCount,
            int chunkCount,
            int chunkEntityCount) {
        transactionTemplate.executeWithoutResult(status -> {
            GraphRagIndexLog persisted = graphRagIndexLogRepository.findById(log.getId()).orElseThrow();
            persisted.markFailed(errorMessage, entityCount, relationshipCount, chunkCount, chunkEntityCount);
            graphRagIndexLogRepository.saveAndFlush(persisted);
            agentWeaveMetrics.recordGraphRagIndex(
                    businessDomain,
                    permissionLevel,
                    persisted.getStatus().name(),
                    persisted.isNeo4jEnabled(),
                    persisted.getDurationMs());
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
            case SKIPPED -> GraphRagIndexSummaryResponse.pending();
        };
    }

    private Instant completedAt(Instant completedAt) {
        return completedAt;
    }
}
