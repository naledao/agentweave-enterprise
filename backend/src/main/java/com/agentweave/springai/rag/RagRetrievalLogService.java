package com.agentweave.springai.rag;

import com.agentweave.observability.application.AgentWeaveMetrics;
import com.agentweave.shared.audit.AuditSummarySanitizer;
import com.agentweave.shared.tracing.CorrelationContext;
import com.agentweave.shared.tracing.TraceContext;
import com.agentweave.shared.tracing.TraceIdProvider;
import com.agentweave.springai.rag.domain.RagRetrievalLog;
import com.agentweave.springai.rag.dto.VectorRagCitationResponse;
import com.agentweave.springai.rag.dto.VectorRagSearchRequest;
import com.agentweave.springai.rag.dto.VectorRagSearchResponse;
import com.agentweave.springai.rag.repository.RagRetrievalLogRepository;
import java.util.DoubleSummaryStatistics;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class RagRetrievalLogService {

    private static final int QUERY_SUMMARY_LENGTH = 1000;
    private static final int SNIPPET_SUMMARY_LENGTH = 280;
    private static final int ERROR_SUMMARY_LENGTH = 1000;
    private static final int SCORE_SUMMARY_LENGTH = 1000;

    private final RagRetrievalLogRepository ragRetrievalLogRepository;
    private final TransactionTemplate transactionTemplate;
    private final TraceIdProvider traceIdProvider;
    private final CorrelationContext correlationContext;
    private final AuditSummarySanitizer auditSummarySanitizer;
    private final AgentWeaveMetrics agentWeaveMetrics;

    public RagRetrievalLogService(
            RagRetrievalLogRepository ragRetrievalLogRepository,
            TransactionTemplate transactionTemplate,
            TraceIdProvider traceIdProvider,
            CorrelationContext correlationContext,
            AuditSummarySanitizer auditSummarySanitizer,
            AgentWeaveMetrics agentWeaveMetrics) {
        this.ragRetrievalLogRepository = ragRetrievalLogRepository;
        this.transactionTemplate = transactionTemplate;
        this.traceIdProvider = traceIdProvider;
        this.correlationContext = correlationContext;
        this.auditSummarySanitizer = auditSummarySanitizer;
        this.agentWeaveMetrics = agentWeaveMetrics;
    }

    public RagRetrievalLog start(
            VectorRagSearchRequest request,
            String retrievalMode,
            Map<String, Object> metadataFilter) {
        TraceContext traceContext = correlationContext.current().orElse(null);
        String traceId = traceContext == null ? traceIdProvider.currentTraceId() : traceContext.traceId();
        UUID conversationId = traceContext == null ? null : traceContext.conversationId();
        UUID messageId = traceContext == null ? null : traceContext.messageId();
        UUID workflowRunId = traceContext == null ? null : traceContext.workflowRunId();
        UUID workflowStepId = traceContext == null ? null : traceContext.workflowStepId();

        return transactionTemplate.execute(status -> ragRetrievalLogRepository.saveAndFlush(
                new RagRetrievalLog(
                        UUID.randomUUID(),
                        conversationId,
                        messageId,
                        workflowRunId,
                        workflowStepId,
                        traceId,
                        auditSummarySanitizer.sanitizeText(request.normalizedQuery(), QUERY_SUMMARY_LENGTH),
                        retrievalMode,
                        metadataFilter,
                        request.normalizedBusinessDomain(),
                        request.normalizedDocumentType(),
                        request.normalizedPermissionLevel(),
                        request.normalizedTimeRange(),
                        request.documentId(),
                        request.normalizedTopK(),
                        request.normalizedSimilarityThreshold())));
    }

    public void markCompleted(RagRetrievalLog log, VectorRagSearchResponse response) {
        transactionTemplate.executeWithoutResult(status -> {
            RagRetrievalLog persisted = ragRetrievalLogRepository.findById(log.getId()).orElseThrow();
            List<VectorRagCitationResponse> citations = response.citations() == null
                    ? List.of()
                    : response.citations();
            persisted.markSuccess(
                    matchedChunkIds(citations),
                    citationSummaries(citations),
                    scoreSummary(citations),
                    citations.size());
            ragRetrievalLogRepository.saveAndFlush(persisted);
            agentWeaveMetrics.recordVectorSearch(
                    persisted.getRetrievalMode(),
                    persisted.getBusinessDomain(),
                    persisted.getPermissionLevel(),
                    persisted.getStatus().name(),
                    persisted.getDurationMs(),
                    citations.size(),
                    persisted.getCitationCount());
        });
    }

    public void markFailed(RagRetrievalLog log, Throwable error) {
        transactionTemplate.executeWithoutResult(status -> {
            RagRetrievalLog persisted = ragRetrievalLogRepository.findById(log.getId()).orElseThrow();
            persisted.markFailed(errorSummary(error));
            ragRetrievalLogRepository.saveAndFlush(persisted);
            agentWeaveMetrics.recordVectorSearch(
                    persisted.getRetrievalMode(),
                    persisted.getBusinessDomain(),
                    persisted.getPermissionLevel(),
                    persisted.getStatus().name(),
                    persisted.getDurationMs(),
                    persisted.getMatchedChunkIds().size(),
                    persisted.getCitationCount());
        });
    }

    private List<String> matchedChunkIds(List<VectorRagCitationResponse> citations) {
        return citations.stream()
                .map(VectorRagCitationResponse::chunkId)
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .toList();
    }

    private List<Map<String, Object>> citationSummaries(List<VectorRagCitationResponse> citations) {
        return citations.stream()
                .map(this::citationSummary)
                .toList();
    }

    private Map<String, Object> citationSummary(VectorRagCitationResponse citation) {
        Map<String, Object> summary = new LinkedHashMap<>();
        putIfPresent(summary, "documentId", citation.documentId());
        putIfPresent(summary, "chunkId", citation.chunkId());
        putIfPresent(summary, "source", citation.source());
        putIfPresent(summary, "score", citation.score());
        putIfPresent(summary, "snippet", auditSummarySanitizer.sanitizeText(
                citation.snippet(),
                SNIPPET_SUMMARY_LENGTH));
        return summary;
    }

    private String scoreSummary(List<VectorRagCitationResponse> citations) {
        DoubleSummaryStatistics statistics = citations.stream()
                .map(VectorRagCitationResponse::score)
                .filter(score -> score != null)
                .mapToDouble(Double::doubleValue)
                .summaryStatistics();
        if (statistics.getCount() == 0) {
            return "count=0";
        }
        String summary = "count=" + statistics.getCount()
                + ";min=" + formatScore(statistics.getMin())
                + ";max=" + formatScore(statistics.getMax())
                + ";avg=" + formatScore(statistics.getAverage());
        return auditSummarySanitizer.sanitizeText(summary, SCORE_SUMMARY_LENGTH);
    }

    private String errorSummary(Throwable error) {
        if (error == null) {
            return null;
        }
        String type = error.getClass().getSimpleName();
        String message = error.getMessage();
        String summary = message == null || message.isBlank() ? type : type + ": " + message;
        return auditSummarySanitizer.sanitizeText(summary, ERROR_SUMMARY_LENGTH);
    }

    private String formatScore(double score) {
        return String.format(java.util.Locale.ROOT, "%.4f", score);
    }

    private void putIfPresent(Map<String, Object> summary, String key, Object value) {
        if (value != null) {
            summary.put(key, value);
        }
    }
}
