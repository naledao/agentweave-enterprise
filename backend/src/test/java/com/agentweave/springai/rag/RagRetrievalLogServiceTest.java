package com.agentweave.springai.rag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.agentweave.observability.application.AgentWeaveMetrics;
import com.agentweave.shared.audit.AuditSummarySanitizer;
import com.agentweave.shared.tracing.CorrelationContext;
import com.agentweave.shared.tracing.TraceContext;
import com.agentweave.shared.tracing.TraceIdProvider;
import com.agentweave.springai.rag.domain.RagRetrievalLog;
import com.agentweave.springai.rag.domain.RagRetrievalStatus;
import com.agentweave.springai.rag.dto.VectorRagCitationResponse;
import com.agentweave.springai.rag.dto.VectorRagSearchRequest;
import com.agentweave.springai.rag.dto.VectorRagSearchResponse;
import com.agentweave.springai.rag.repository.RagRetrievalLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

class RagRetrievalLogServiceTest {

    private final RagRetrievalLogRepository repository =
            org.mockito.Mockito.mock(RagRetrievalLogRepository.class);
    private final TraceIdProvider traceIdProvider =
            org.mockito.Mockito.mock(TraceIdProvider.class);
    private final CorrelationContext correlationContext =
            org.mockito.Mockito.mock(CorrelationContext.class);
    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final RagRetrievalLogService service = new RagRetrievalLogService(
            repository,
            new TransactionTemplate(new ImmediateTransactionManager()),
            traceIdProvider,
            correlationContext,
            new AuditSummarySanitizer(new ObjectMapper()),
            new AgentWeaveMetrics(meterRegistry));

    private UUID conversationId;
    private UUID messageId;
    private UUID workflowRunId;
    private UUID workflowStepId;

    @BeforeEach
    void setUp() {
        conversationId = UUID.randomUUID();
        messageId = UUID.randomUUID();
        workflowRunId = UUID.randomUUID();
        workflowStepId = UUID.randomUUID();
        when(correlationContext.current())
                .thenReturn(Optional.of(new TraceContext(
                        "trace-rag-test",
                        conversationId,
                        messageId,
                        workflowRunId,
                        workflowStepId)));
        when(repository.saveAndFlush(any(RagRetrievalLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0, RagRetrievalLog.class));
        when(repository.findById(any()))
                .thenAnswer(invocation -> Optional.of(startedLog(invocation.getArgument(0, UUID.class))));
    }

    @Test
    void startStoresCorrelationAndSanitizedRequestSummary() {
        RagRetrievalLog log = service.start(
                request("password=secret query", "order", "RUNBOOK", "INTERNAL"),
                RagRetrievalMode.VECTOR_ONLY.name(),
                Map.of("businessDomain", "order"));

        assertThat(log.getConversationId()).isEqualTo(conversationId);
        assertThat(log.getMessageId()).isEqualTo(messageId);
        assertThat(log.getWorkflowRunId()).isEqualTo(workflowRunId);
        assertThat(log.getWorkflowStepId()).isEqualTo(workflowStepId);
        assertThat(log.getTraceId()).isEqualTo("trace-rag-test");
        assertThat(log.getQuery()).isEqualTo("password=****** query");
        assertThat(log.getMetadataFilter()).containsEntry("businessDomain", "order");
        assertThat(log.getStatus()).isEqualTo(RagRetrievalStatus.PROCESSING);
    }

    @Test
    void markCompletedStoresChunkScoreCitationSummariesAndMetrics() {
        UUID logId = UUID.randomUUID();
        RagRetrievalLog log = startedLog(logId);
        when(repository.findById(logId)).thenReturn(Optional.of(log));
        VectorRagSearchResponse response = new VectorRagSearchResponse(
                "order timeout",
                RagRetrievalMode.VECTOR_ONLY.name(),
                3,
                0.25,
                Map.of("businessDomain", "order"),
                List.of(
                        citation("doc-1", "chunk-1", 0.91, "password=secret " + "x".repeat(400)),
                        citation("doc-2", "chunk-2", 0.73, "normal snippet")));

        service.markCompleted(log, response);

        assertThat(log.getStatus()).isEqualTo(RagRetrievalStatus.SUCCESS);
        assertThat(log.getMatchedChunkIds()).containsExactly("chunk-1", "chunk-2");
        assertThat(log.getCitationCount()).isEqualTo(2);
        assertThat(log.getScoreSummary()).contains("count=2", "min=0.7300", "max=0.9100");
        assertThat(log.getCitationSummaries().get(0).get("snippet").toString())
                .startsWith("password=******")
                .hasSizeLessThanOrEqualTo(280);
        assertThat(meterRegistry.find("agentweave.vector.search.duration")
                        .tag("retrievalMode", "VECTOR_ONLY")
                        .tag("businessDomain", "order")
                        .tag("permissionLevel", "INTERNAL")
                        .tag("status", "SUCCESS")
                        .timer())
                .isNotNull();
        assertThat(meterRegistry.find("agentweave.vector.search.count")
                        .tag("status", "SUCCESS")
                        .counter()
                        .count())
                .isEqualTo(1);
        assertThat(meterRegistry.find("agentweave.vector.search.matches")
                        .tag("status", "SUCCESS")
                        .counter()
                        .count())
                .isEqualTo(2);
        assertThat(meterRegistry.find("agentweave.rag.citation.count")
                        .tag("status", "SUCCESS")
                        .counter()
                        .count())
                .isEqualTo(2);
    }

    @Test
    void markFailedStoresErrorSummaryAndFailureMetric() {
        UUID logId = UUID.randomUUID();
        RagRetrievalLog log = startedLog(logId);
        when(repository.findById(logId)).thenReturn(Optional.of(log));

        service.markFailed(log, new IllegalStateException("token=secret vector store unavailable"));

        assertThat(log.getStatus()).isEqualTo(RagRetrievalStatus.FAILED);
        assertThat(log.getErrorMessage()).isEqualTo("IllegalStateException: token=****** vector store unavailable");
        assertThat(meterRegistry.find("agentweave.vector.search.failures")
                        .tag("status", "FAILED")
                        .counter()
                        .count())
                .isEqualTo(1);
    }

    private RagRetrievalLog startedLog(UUID id) {
        return new RagRetrievalLog(
                id,
                conversationId,
                messageId,
                workflowRunId,
                workflowStepId,
                "trace-rag-test",
                "order timeout",
                RagRetrievalMode.VECTOR_ONLY.name(),
                Map.of("businessDomain", "order"),
                "order",
                "RUNBOOK",
                "INTERNAL",
                null,
                null,
                3,
                0.25);
    }

    private VectorRagSearchRequest request(
            String query,
            String businessDomain,
            String documentType,
            String permissionLevel) {
        return new VectorRagSearchRequest(
                query,
                businessDomain,
                documentType,
                permissionLevel,
                null,
                java.time.Instant.parse("2026-01-01T00:00:00Z"),
                java.time.Instant.parse("2026-12-31T23:59:59Z"),
                3,
                0.25);
    }

    private VectorRagCitationResponse citation(String documentId, String chunkId, double score, String snippet) {
        return new VectorRagCitationResponse(
                documentId,
                chunkId,
                "runbook",
                "order",
                "RUNBOOK",
                "INTERNAL",
                score,
                snippet,
                Map.of());
    }

    private static final class ImmediateTransactionManager implements PlatformTransactionManager {

        @Override
        public TransactionStatus getTransaction(TransactionDefinition definition) {
            return new SimpleTransactionStatus();
        }

        @Override
        public void commit(TransactionStatus status) {
        }

        @Override
        public void rollback(TransactionStatus status) {
        }
    }
}
