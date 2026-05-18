package com.agentweave.springai.rag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agentweave.shared.security.CurrentUserService;
import com.agentweave.springai.rag.domain.RagRetrievalLog;
import com.agentweave.springai.rag.dto.VectorRagSearchRequest;
import com.agentweave.springai.rag.dto.VectorRagSearchResponse;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

class VectorRetrievalServiceTest {

    private final VectorStore vectorStore = org.mockito.Mockito.mock(VectorStore.class);
    private final CurrentUserService currentUserService = org.mockito.Mockito.mock(CurrentUserService.class);
    private final RagRetrievalLogService ragRetrievalLogService = org.mockito.Mockito.mock(RagRetrievalLogService.class);
    private final RagMetadataFilterFactory metadataFilterFactory = new RagMetadataFilterFactory();
    private final VectorRetrievalService service = new VectorRetrievalService(
            vectorStore,
            metadataFilterFactory,
            currentUserService,
            ragRetrievalLogService);

    @Test
    void searchRecordsHybridRetrievalLogOnSuccess() {
        RagRetrievalLog log = startedLog();
        when(ragRetrievalLogService.start(any(), eq(RagRetrievalMode.HYBRID.name()), any())).thenReturn(log);
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(List.of(Document.builder()
                        .id("chunk-1")
                        .text("restart retry worker")
                        .metadata(Map.of(
                                "documentId", UUID.randomUUID().toString(),
                                "chunkId", "chunk-1",
                                "businessDomain", "order",
                                "documentType", "RUNBOOK",
                                "permissionLevel", "INTERNAL"))
                        .score(0.88)
                        .build()));

        VectorRagSearchResponse response = service.search(request(), RagRetrievalMode.HYBRID);

        assertThat(response.retrievalMode()).isEqualTo("HYBRID");
        assertThat(response.citations()).hasSize(1);
        verify(ragRetrievalLogService).markCompleted(eq(log), any(VectorRagSearchResponse.class));
    }

    @Test
    void searchRecordsFailedRetrievalLogAndRethrows() {
        RagRetrievalLog log = startedLog();
        IllegalStateException failure = new IllegalStateException("vector store unavailable");
        when(ragRetrievalLogService.start(any(), eq(RagRetrievalMode.VECTOR_ONLY.name()), any())).thenReturn(log);
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenThrow(failure);

        assertThatThrownBy(() -> service.search(request()))
                .isSameAs(failure);

        ArgumentCaptor<Throwable> errorCaptor = ArgumentCaptor.forClass(Throwable.class);
        verify(ragRetrievalLogService).markFailed(eq(log), errorCaptor.capture());
        assertThat(errorCaptor.getValue()).isSameAs(failure);
    }

    private VectorRagSearchRequest request() {
        return new VectorRagSearchRequest(
                "order timeout",
                "order",
                "RUNBOOK",
                "INTERNAL",
                null,
                null,
                null,
                3,
                0.25);
    }

    private RagRetrievalLog startedLog() {
        return new RagRetrievalLog(
                UUID.randomUUID(),
                null,
                null,
                null,
                null,
                "trace-vector-test",
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
}
