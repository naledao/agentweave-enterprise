package com.agentweave.graphrag.application;

import com.agentweave.graphrag.domain.GraphRagRetrievalLog;
import com.agentweave.graphrag.domain.KnowledgeGraphEntity;
import com.agentweave.graphrag.domain.KnowledgeGraphEntityAlias;
import com.agentweave.graphrag.domain.KnowledgeGraphRelationship;
import com.agentweave.graphrag.dto.GraphRagRetrievalRequest;
import com.agentweave.graphrag.dto.GraphRagRetrievalResponse;
import com.agentweave.conversation.application.ConversationPrompt;
import com.agentweave.shared.security.CurrentUserService;
import com.agentweave.shared.tracing.CorrelationContext;
import com.agentweave.shared.tracing.TraceIdProvider;
import com.agentweave.springai.rag.dto.VectorRagCitationResponse;
import com.agentweave.springai.rag.dto.VectorRagSearchResponse;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

@Service
public class GraphRagRetrievalService {

    private static final Logger log = LoggerFactory.getLogger(GraphRagRetrievalService.class);
    private static final String SEARCH_PERMISSION = "knowledge:rag:search";
    private static final int DEFAULT_MAX_DEPTH = 2;
    private static final int DEFAULT_MAX_PATH_COUNT = 5;

    private final CurrentUserService currentUserService;
    private final GraphRagRetrievalLogService graphRagRetrievalLogService;
    private final GraphPathSearchService graphPathSearchService;
    private final com.agentweave.graphrag.repository.KnowledgeGraphEntityRepository knowledgeGraphEntityRepository;
    private final com.agentweave.graphrag.repository.KnowledgeGraphEntityAliasRepository knowledgeGraphEntityAliasRepository;
    private final com.agentweave.graphrag.repository.KnowledgeGraphRelationshipRepository knowledgeGraphRelationshipRepository;
    private final TraceIdProvider traceIdProvider;

    public GraphRagRetrievalService(
            CurrentUserService currentUserService,
            GraphRagRetrievalLogService graphRagRetrievalLogService,
            GraphPathSearchService graphPathSearchService,
            com.agentweave.graphrag.repository.KnowledgeGraphEntityRepository knowledgeGraphEntityRepository,
            com.agentweave.graphrag.repository.KnowledgeGraphEntityAliasRepository knowledgeGraphEntityAliasRepository,
            com.agentweave.graphrag.repository.KnowledgeGraphRelationshipRepository knowledgeGraphRelationshipRepository,
            TraceIdProvider traceIdProvider) {
        this.currentUserService = currentUserService;
        this.graphRagRetrievalLogService = graphRagRetrievalLogService;
        this.graphPathSearchService = graphPathSearchService;
        this.knowledgeGraphEntityRepository = knowledgeGraphEntityRepository;
        this.knowledgeGraphEntityAliasRepository = knowledgeGraphEntityAliasRepository;
        this.knowledgeGraphRelationshipRepository = knowledgeGraphRelationshipRepository;
        this.traceIdProvider = traceIdProvider;
    }

    public GraphRagRetrievalResponse retrieve(ConversationPrompt prompt, VectorRagSearchResponse vectorResponse) {
        currentUserService.requireCurrentUser();
        currentUserService.requirePermission(SEARCH_PERMISSION);

        GraphRagRetrievalRequest request = buildRequest(prompt, vectorResponse);
        GraphRagRetrievalLog logEntry = graphRagRetrievalLogService.start(
                prompt.conversationId(),
                currentMessageId(),
                currentTraceId(),
                request.normalizedQuery(),
                request.normalizedBusinessDomain(),
                request.normalizedPermissionLevel(),
                request.documentId(),
                request.maxDepth(),
                request.maxPathCount(),
                List.of());
        try {
            GraphRagRetrievalResponse response = graphPathSearchService.search(
                    request,
                    knowledgeGraphEntityRepository.findAll(),
                    knowledgeGraphEntityAliasRepository.findAll(),
                    knowledgeGraphRelationshipRepository.findAll());
            graphRagRetrievalLogService.markCompleted(logEntry, response);
            return response;
        } catch (RuntimeException ex) {
            String summary = errorSummary(ex);
            graphRagRetrievalLogService.markFailed(logEntry, summary);
            log.warn(
                    "GraphRAG retrieval failed: conversationId={}, traceId={}, error={}",
                    prompt.conversationId(),
                    requestTraceId(),
                    summary,
                    ex);
            return GraphRagRetrievalResponse.empty();
        }
    }

    private GraphRagRetrievalRequest buildRequest(ConversationPrompt prompt, VectorRagSearchResponse vectorResponse) {
        List<VectorRagCitationResponse> citations = vectorResponse == null || vectorResponse.citations() == null
                ? List.of()
                : vectorResponse.citations();
        return new GraphRagRetrievalRequest(
                prompt.latestUserMessage(),
                uniqueValue(citations, VectorRagCitationResponse::businessDomain),
                uniqueValue(citations, VectorRagCitationResponse::permissionLevel),
                uniqueDocumentId(citations),
                DEFAULT_MAX_DEPTH,
                DEFAULT_MAX_PATH_COUNT);
    }

    private String uniqueValue(List<VectorRagCitationResponse> citations, java.util.function.Function<VectorRagCitationResponse, String> extractor) {
        List<String> values = citations.stream()
                .map(extractor)
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .toList();
        return values.size() == 1 ? values.get(0) : null;
    }

    private UUID uniqueDocumentId(List<VectorRagCitationResponse> citations) {
        List<UUID> ids = citations.stream()
                .map(VectorRagCitationResponse::documentId)
                .filter(value -> value != null && !value.isBlank())
                .map(UUID::fromString)
                .distinct()
                .toList();
        return ids.size() == 1 ? ids.get(0) : null;
    }

    private String currentTraceId() {
        String traceId = MDC.get(TraceIdProvider.TRACE_ID_KEY);
        return traceId == null || traceId.isBlank() ? traceIdProvider.currentTraceId() : traceId;
    }

    private UUID currentMessageId() {
        String messageId = MDC.get(CorrelationContext.MESSAGE_ID_KEY);
        if (messageId == null || messageId.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(messageId);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private String requestTraceId() {
        String traceId = MDC.get(TraceIdProvider.TRACE_ID_KEY);
        return traceId == null || traceId.isBlank() ? "unknown" : traceId;
    }

    private String errorSummary(Exception ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            message = ex.getClass().getSimpleName();
        }
        return message.length() > 1000 ? message.substring(0, 1000) : message;
    }
}
