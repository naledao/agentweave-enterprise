package com.agentweave.graphrag.application;

import com.agentweave.graphrag.domain.GraphRagRetrievalLog;
import com.agentweave.graphrag.repository.GraphRagRetrievalLogRepository;
import com.agentweave.graphrag.dto.GraphRagRetrievalResponse;
import com.agentweave.observability.application.AgentWeaveMetrics;
import com.agentweave.shared.tracing.CorrelationContext;
import com.agentweave.shared.tracing.TraceContext;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class GraphRagRetrievalLogService {

    private final GraphRagRetrievalLogRepository graphRagRetrievalLogRepository;
    private final TransactionTemplate transactionTemplate;
    private final AgentWeaveMetrics agentWeaveMetrics;
    private final CorrelationContext correlationContext;

    public GraphRagRetrievalLogService(
            GraphRagRetrievalLogRepository graphRagRetrievalLogRepository,
            TransactionTemplate transactionTemplate,
            AgentWeaveMetrics agentWeaveMetrics,
            CorrelationContext correlationContext) {
        this.graphRagRetrievalLogRepository = graphRagRetrievalLogRepository;
        this.transactionTemplate = transactionTemplate;
        this.agentWeaveMetrics = agentWeaveMetrics;
        this.correlationContext = correlationContext;
    }

    public GraphRagRetrievalLog start(
            UUID conversationId,
            UUID messageId,
            String traceId,
            String query,
            String businessDomain,
            String permissionLevel,
            UUID documentId,
            String retrievalMode,
            int maxDepth,
            int maxPathCount,
            List<String> resolvedEntities) {
        TraceContext traceContext = correlationContext.current().orElse(null);
        UUID workflowRunId = traceContext == null ? null : traceContext.workflowRunId();
        UUID workflowStepId = traceContext == null ? null : traceContext.workflowStepId();
        return transactionTemplate.execute(status -> graphRagRetrievalLogRepository.saveAndFlush(
                new GraphRagRetrievalLog(
                        UUID.randomUUID(),
                        conversationId,
                        messageId,
                        workflowRunId,
                        workflowStepId,
                        traceId,
                        query,
                        retrievalMode,
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
            agentWeaveMetrics.recordGraphRagPathSearch(
                    persisted.getRetrievalMode(),
                    persisted.getBusinessDomain(),
                    persisted.getPermissionLevel(),
                    persisted.getStatus().name(),
                    persisted.getDurationMs(),
                    persisted.getFilteredPathCount());
        });
    }

    public void markFailed(GraphRagRetrievalLog log, String errorMessage) {
        transactionTemplate.executeWithoutResult(status -> {
            GraphRagRetrievalLog persisted = graphRagRetrievalLogRepository.findById(log.getId()).orElseThrow();
            persisted.markFailed(errorMessage);
            graphRagRetrievalLogRepository.saveAndFlush(persisted);
            agentWeaveMetrics.recordGraphRagPathSearch(
                    persisted.getRetrievalMode(),
                    persisted.getBusinessDomain(),
                    persisted.getPermissionLevel(),
                    persisted.getStatus().name(),
                    persisted.getDurationMs(),
                    persisted.getFilteredPathCount());
        });
    }

    public void markDegraded(GraphRagRetrievalLog log, String errorMessage) {
        transactionTemplate.executeWithoutResult(status -> {
            GraphRagRetrievalLog persisted = graphRagRetrievalLogRepository.findById(log.getId()).orElseThrow();
            persisted.markDegraded(errorMessage);
            graphRagRetrievalLogRepository.saveAndFlush(persisted);
            agentWeaveMetrics.recordGraphRagPathSearch(
                    persisted.getRetrievalMode(),
                    persisted.getBusinessDomain(),
                    persisted.getPermissionLevel(),
                    persisted.getStatus().name(),
                    persisted.getDurationMs(),
                    persisted.getFilteredPathCount());
        });
    }
}
