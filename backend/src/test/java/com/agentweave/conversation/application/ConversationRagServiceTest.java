package com.agentweave.conversation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agentweave.graphrag.application.GraphRagRetrievalService;
import com.agentweave.graphrag.dto.GraphPathResponse;
import com.agentweave.graphrag.dto.GraphRagRetrievalResponse;
import com.agentweave.shared.security.CurrentUser;
import com.agentweave.shared.security.CurrentUserService;
import com.agentweave.springai.rag.RagRetrievalMode;
import com.agentweave.springai.rag.QueryRouterService;
import com.agentweave.springai.rag.VectorRetrievalService;
import com.agentweave.springai.rag.dto.VectorRagCitationResponse;
import com.agentweave.springai.rag.dto.VectorRagSearchRequest;
import com.agentweave.springai.rag.dto.VectorRagSearchResponse;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ConversationRagServiceTest {

    private final VectorRetrievalService vectorRetrievalService = org.mockito.Mockito.mock(VectorRetrievalService.class);
    private final GraphRagRetrievalService graphRagRetrievalService =
            org.mockito.Mockito.mock(GraphRagRetrievalService.class);
    private final CurrentUserService currentUserService = org.mockito.Mockito.mock(CurrentUserService.class);
    private final ConversationRagService conversationRagService = new ConversationRagService(
            vectorRetrievalService,
            graphRagRetrievalService,
            new RagContextMerger(),
            currentUserService,
            new QueryRouterService());

    @Test
    void vectorOnlyQuestionDoesNotTriggerGraphRetrieval() {
        CurrentUser user = currentUser();
        when(currentUserService.getCurrentUser()).thenReturn(Optional.of(user));
        when(currentUserService.hasPermission("knowledge:rag:search")).thenReturn(true);
        when(vectorRetrievalService.search(any(VectorRagSearchRequest.class), eq(RagRetrievalMode.VECTOR_ONLY)))
                .thenReturn(vectorResponse());

        RagPromptContext context = conversationRagService.retrieve(prompt("\u8ba2\u5355\u63a5\u53e3\u8d85\u65f6\u600e\u4e48\u6392\u67e5"));

        assertThat(context.retrievalMode()).isEqualTo("VECTOR_ONLY");
        assertThat(context.citations()).hasSize(1);
        assertThat(context.graphPaths()).isEmpty();
        verify(vectorRetrievalService).search(any(VectorRagSearchRequest.class), eq(RagRetrievalMode.VECTOR_ONLY));
        verify(graphRagRetrievalService, never()).retrieve(any(), any());
    }

    @Test
    void hybridQuestionTriggersVectorAndGraphRetrieval() {
        CurrentUser user = currentUser();
        when(currentUserService.getCurrentUser()).thenReturn(Optional.of(user));
        when(currentUserService.hasPermission("knowledge:rag:search")).thenReturn(true);
        when(vectorRetrievalService.search(any(VectorRagSearchRequest.class), eq(RagRetrievalMode.HYBRID)))
                .thenReturn(vectorResponse());
        when(graphRagRetrievalService.retrieve(any(ConversationPrompt.class), any(VectorRagSearchResponse.class)))
                .thenReturn(new GraphRagRetrievalResponse(
                        List.of(new GraphPathResponse(
                                "path-1",
                                1,
                                List.of("Order Service", "Payment API"),
                                List.of("CALLS"),
                                List.of("chunk-1"),
                                0.91d)),
                        List.of("Order Service", "Payment API"),
                        List.of("chunk-1"),
                        "count=1",
                        1,
                        0));

        RagPromptContext context = conversationRagService.retrieve(prompt("\u652f\u4ed8\u5931\u8d25\u4f1a\u5f71\u54cd\u54ea\u4e9b\u4e0a\u4e0b\u6e38\u670d\u52a1"));

        assertThat(context.retrievalMode()).isEqualTo("HYBRID");
        assertThat(context.citations()).hasSize(1);
        assertThat(context.graphPaths()).hasSize(1);
        ArgumentCaptor<VectorRagSearchRequest> requestCaptor = ArgumentCaptor.forClass(VectorRagSearchRequest.class);
        verify(vectorRetrievalService).search(requestCaptor.capture(), eq(RagRetrievalMode.HYBRID));
        assertThat(requestCaptor.getValue().topK()).isEqualTo(5);
        assertThat(requestCaptor.getValue().similarityThreshold()).isEqualTo(0.0);
        verify(graphRagRetrievalService).retrieve(any(ConversationPrompt.class), any(VectorRagSearchResponse.class));
    }

    @Test
    void missingPermissionSkipsRetrieval() {
        when(currentUserService.getCurrentUser()).thenReturn(Optional.of(currentUser()));
        when(currentUserService.hasPermission("knowledge:rag:search")).thenReturn(false);

        RagPromptContext context = conversationRagService.retrieve(prompt("\u4efb\u610f\u95ee\u9898"));

        assertThat(context.retrievalMode()).isEqualTo("VECTOR_ONLY");
        assertThat(context.citations()).isEmpty();
        assertThat(context.graphPaths()).isEmpty();
        verify(vectorRetrievalService, never()).search(any());
        verify(graphRagRetrievalService, never()).retrieve(any(), any());
    }

    private ConversationPrompt prompt(String query) {
        return new ConversationPrompt(UUID.randomUUID(), "Demo", query, List.of());
    }

    private VectorRagSearchResponse vectorResponse() {
        return new VectorRagSearchResponse(
                "query",
                "VECTOR_ONLY",
                5,
                0.0,
                Map.of(),
                List.of(new VectorRagCitationResponse(
                        UUID.randomUUID().toString(),
                        UUID.randomUUID().toString(),
                        "runbook",
                        "order",
                        "RUNBOOK",
                        "INTERNAL",
                        0.88d,
                        "restart retry worker",
                        Map.of())));
    }

    private CurrentUser currentUser() {
        return new CurrentUser(
                UUID.randomUUID(),
                "rag-user",
                "RAG User",
                Set.of("RAG_SEARCHER"),
                Set.of("knowledge:rag:search"));
    }
}
