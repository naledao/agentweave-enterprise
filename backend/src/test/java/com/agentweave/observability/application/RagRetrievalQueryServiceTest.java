package com.agentweave.observability.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.agentweave.observability.dto.RagRetrievalListResponse;
import com.agentweave.observability.dto.RagRetrievalQueryRequest;
import com.agentweave.observability.dto.RagRetrievalResponse;
import com.agentweave.shared.exception.ResourceNotFoundException;
import com.agentweave.shared.security.CurrentUser;
import com.agentweave.shared.security.CurrentUserService;
import com.agentweave.springai.rag.domain.RagRetrievalLog;
import com.agentweave.springai.rag.domain.RagRetrievalStatus;
import com.agentweave.springai.rag.repository.RagRetrievalLogRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

class RagRetrievalQueryServiceTest {

    private final RagRetrievalLogRepository repository =
            org.mockito.Mockito.mock(RagRetrievalLogRepository.class);
    private final CurrentUserService currentUserService =
            org.mockito.Mockito.mock(CurrentUserService.class);
    private final RagRetrievalQueryService service = new RagRetrievalQueryService(repository, currentUserService);

    @Test
    void listReturnsPagedRetrievalLogsForObserver() {
        CurrentUser observer = new CurrentUser(
                UUID.randomUUID(),
                "observer",
                "Observer",
                Set.of(),
                Set.of("observability:read"));
        RagRetrievalLog log = completedLog();
        when(currentUserService.requireCurrentUser()).thenReturn(observer);
        when(repository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(log)));

        RagRetrievalListResponse response = service.list(new RagRetrievalQueryRequest(
                0,
                10,
                "HYBRID",
                "order",
                "RUNBOOK",
                "INTERNAL",
                RagRetrievalStatus.SUCCESS,
                log.getConversationId(),
                log.getMessageId(),
                log.getWorkflowRunId(),
                log.getWorkflowStepId(),
                "trace-rag-query",
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-12-31T23:59:59Z")));

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).id()).isEqualTo(log.getId());
        assertThat(response.items().get(0).retrievalMode()).isEqualTo("HYBRID");
        assertThat(response.items().get(0).workflowRunId()).isEqualTo(log.getWorkflowRunId());
        assertThat(response.items().get(0).workflowStepId()).isEqualTo(log.getWorkflowStepId());
        assertThat(response.items().get(0).metadataFilter()).containsEntry("businessDomain", "order");
    }

    @Test
    void getReturnsDetailWhenReadable() {
        when(currentUserService.requireCurrentUser()).thenReturn(admin());
        RagRetrievalLog log = completedLog();
        when(repository.findOne(any(Specification.class))).thenReturn(Optional.of(log));

        RagRetrievalResponse response = service.get(log.getId());

        assertThat(response.id()).isEqualTo(log.getId());
        assertThat(response.citationCount()).isEqualTo(1);
    }

    @Test
    void getThrowsNotFoundWhenUnreadableOrMissing() {
        when(currentUserService.requireCurrentUser()).thenReturn(admin());
        when(repository.findOne(any(Specification.class))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("RAG retrieval log not found");
    }

    private CurrentUser admin() {
        return new CurrentUser(
                UUID.randomUUID(),
                "admin",
                "Admin",
                Set.of("ADMIN"),
                Set.of());
    }

    private RagRetrievalLog completedLog() {
        UUID conversationId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        UUID workflowRunId = UUID.randomUUID();
        UUID workflowStepId = UUID.randomUUID();
        RagRetrievalLog log = new RagRetrievalLog(
                UUID.randomUUID(),
                conversationId,
                messageId,
                workflowRunId,
                workflowStepId,
                "trace-rag-query",
                "order timeout",
                "HYBRID",
                Map.of("businessDomain", "order"),
                "order",
                "RUNBOOK",
                "INTERNAL",
                "2026-01-01T00:00:00Z..2026-12-31T23:59:59Z",
                null,
                5,
                0.0);
        log.markSuccess(
                List.of("chunk-1"),
                List.of(Map.of("chunkId", "chunk-1", "score", 0.91d, "snippet", "restart worker")),
                "count=1;min=0.9100;max=0.9100;avg=0.9100",
                1);
        return log;
    }
}
