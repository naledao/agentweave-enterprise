package com.agentweave.observability.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.agentweave.graphrag.domain.GraphRagIndexLog;
import com.agentweave.graphrag.domain.GraphRagIndexStatus;
import com.agentweave.graphrag.domain.GraphRagRetrievalLog;
import com.agentweave.graphrag.domain.GraphRagRetrievalStatus;
import com.agentweave.graphrag.dto.GraphRagRetrievalResponse;
import com.agentweave.graphrag.repository.GraphRagIndexLogRepository;
import com.agentweave.graphrag.repository.GraphRagRetrievalLogRepository;
import com.agentweave.observability.dto.GraphRagIndexLogListResponse;
import com.agentweave.observability.dto.GraphRagIndexLogQueryRequest;
import com.agentweave.observability.dto.GraphRagRetrievalLogListResponse;
import com.agentweave.observability.dto.GraphRagRetrievalLogQueryRequest;
import com.agentweave.observability.dto.GraphRagSummaryResponse;
import com.agentweave.shared.security.CurrentUser;
import com.agentweave.shared.security.CurrentUserService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

class GraphRagObservabilityQueryServiceTest {

    private final GraphRagIndexLogRepository indexLogRepository =
            org.mockito.Mockito.mock(GraphRagIndexLogRepository.class);
    private final GraphRagRetrievalLogRepository retrievalLogRepository =
            org.mockito.Mockito.mock(GraphRagRetrievalLogRepository.class);
    private final CurrentUserService currentUserService =
            org.mockito.Mockito.mock(CurrentUserService.class);
    private final GraphRagObservabilityQueryService service = new GraphRagObservabilityQueryService(
            indexLogRepository,
            retrievalLogRepository,
            currentUserService);

    @Test
    void summaryReturnsLatestReadableLogsAndCounts() {
        GraphRagIndexLog indexLog = completedIndexLog();
        GraphRagRetrievalLog retrievalLog = completedRetrievalLog();
        when(currentUserService.requireCurrentUser()).thenReturn(observer());
        when(indexLogRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(indexLog)));
        when(retrievalLogRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(retrievalLog)));
        when(indexLogRepository.count(any(Specification.class))).thenReturn(3L);
        when(retrievalLogRepository.count(any(Specification.class))).thenReturn(5L);

        GraphRagSummaryResponse response = service.summary();

        assertThat(response.latestIndexLog().traceId()).isEqualTo(indexLog.getTraceId());
        assertThat(response.latestRetrievalLog().traceId()).isEqualTo(retrievalLog.getTraceId());
        assertThat(response.indexLogCount()).isEqualTo(3);
        assertThat(response.retrievalLogCount()).isEqualTo(5);
    }

    @Test
    void listIndexLogsReturnsPagedItems() {
        GraphRagIndexLog indexLog = completedIndexLog();
        when(currentUserService.requireCurrentUser()).thenReturn(observer());
        when(indexLogRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(indexLog)));

        GraphRagIndexLogListResponse response = service.listIndexLogs(new GraphRagIndexLogQueryRequest(
                0,
                10,
                indexLog.getDocumentId(),
                indexLog.getTraceId(),
                GraphRagIndexStatus.INDEXED,
                false,
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-12-31T23:59:59Z")));

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).entityCount()).isEqualTo(2);
        assertThat(response.items().get(0).chunkEntityCount()).isEqualTo(4);
    }

    @Test
    void listRetrievalLogsReturnsPagedItems() {
        GraphRagRetrievalLog retrievalLog = completedRetrievalLog();
        when(currentUserService.requireCurrentUser()).thenReturn(observer());
        when(retrievalLogRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(retrievalLog)));

        GraphRagRetrievalLogListResponse response = service.listRetrievalLogs(new GraphRagRetrievalLogQueryRequest(
                0,
                10,
                "HYBRID",
                "order",
                "INTERNAL",
                GraphRagRetrievalStatus.SUCCESS,
                retrievalLog.getConversationId(),
                retrievalLog.getMessageId(),
                retrievalLog.getWorkflowRunId(),
                retrievalLog.getWorkflowStepId(),
                retrievalLog.getDocumentId(),
                retrievalLog.getTraceId(),
                null,
                null));

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).workflowRunId()).isEqualTo(retrievalLog.getWorkflowRunId());
        assertThat(response.items().get(0).workflowStepId()).isEqualTo(retrievalLog.getWorkflowStepId());
        assertThat(response.items().get(0).filteredPathCount()).isEqualTo(2);
        assertThat(response.items().get(0).sourceChunkIds()).containsExactly("chunk-1", "chunk-2");
    }

    private CurrentUser observer() {
        return new CurrentUser(
                UUID.randomUUID(),
                "observer",
                "Observer",
                Set.of(),
                Set.of("observability:read"));
    }

    private GraphRagIndexLog completedIndexLog() {
        GraphRagIndexLog log = new GraphRagIndexLog(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "trace-graphrag-index-query",
                2,
                false);
        log.markCompleted(2, 1, 2, 4);
        return log;
    }

    private GraphRagRetrievalLog completedRetrievalLog() {
        GraphRagRetrievalLog log = new GraphRagRetrievalLog(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "trace-graphrag-retrieval-query",
                "order timeout",
                "HYBRID",
                "order",
                "INTERNAL",
                UUID.randomUUID(),
                2,
                5,
                List.of("Order Service"));
        log.markCompleted(
                3,
                2,
                List.of("Order Service"),
                List.of("chunk-1", "chunk-2"),
                "count=2");
        return log;
    }
}
