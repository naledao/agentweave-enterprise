package com.agentweave.springai.rag;

import com.agentweave.shared.security.CurrentUserService;
import com.agentweave.shared.audit.AuditEventType;
import com.agentweave.shared.audit.AuditLog;
import com.agentweave.springai.rag.dto.VectorRagCitationResponse;
import com.agentweave.springai.rag.dto.VectorRagSearchRequest;
import com.agentweave.springai.rag.dto.VectorRagSearchResponse;
import com.agentweave.springai.rag.domain.RagRetrievalLog;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

@Service
public class VectorRetrievalService {

    private static final String SEARCH_PERMISSION = "knowledge:rag:search";

    private final VectorStore vectorStore;
    private final RagMetadataFilterFactory metadataFilterFactory;
    private final CurrentUserService currentUserService;
    private final RagRetrievalLogService ragRetrievalLogService;

    public VectorRetrievalService(
            VectorStore vectorStore,
            RagMetadataFilterFactory metadataFilterFactory,
            CurrentUserService currentUserService,
            RagRetrievalLogService ragRetrievalLogService) {
        this.vectorStore = vectorStore;
        this.metadataFilterFactory = metadataFilterFactory;
        this.currentUserService = currentUserService;
        this.ragRetrievalLogService = ragRetrievalLogService;
    }

    @AuditLog(
            eventType = AuditEventType.RAG_RETRIEVAL,
            resourceType = "rag",
            resourceId = "#request.documentId",
            includeResponse = false)
    public VectorRagSearchResponse search(VectorRagSearchRequest request) {
        return search(request, RagRetrievalMode.VECTOR_ONLY);
    }

    @AuditLog(
            eventType = AuditEventType.RAG_RETRIEVAL,
            resourceType = "rag",
            resourceId = "#request.documentId",
            includeResponse = false)
    public VectorRagSearchResponse search(VectorRagSearchRequest request, RagRetrievalMode retrievalMode) {
        currentUserService.requireCurrentUser();
        currentUserService.requirePermission(SEARCH_PERMISSION);

        RagRetrievalMode normalizedRetrievalMode = retrievalMode == null ? RagRetrievalMode.VECTOR_ONLY : retrievalMode;
        Map<String, Object> filter = metadataFilterFactory.describe(request);
        RagRetrievalLog retrievalLog = ragRetrievalLogService.start(
                request,
                normalizedRetrievalMode.name(),
                filter);
        SearchRequest.Builder searchRequest = SearchRequest.builder()
                .query(request.normalizedQuery())
                .topK(request.normalizedTopK())
                .similarityThreshold(request.normalizedSimilarityThreshold());
        metadataFilterFactory.build(request).ifPresent(searchRequest::filterExpression);

        try {
            List<VectorRagCitationResponse> citations = vectorStore.similaritySearch(searchRequest.build())
                    .stream()
                    .map(this::toCitation)
                    .toList();

            VectorRagSearchResponse response = new VectorRagSearchResponse(
                    request.normalizedQuery(),
                    normalizedRetrievalMode.name(),
                    request.normalizedTopK(),
                    request.normalizedSimilarityThreshold(),
                    filter,
                    citations);
            ragRetrievalLogService.markCompleted(retrievalLog, response);
            return response;
        } catch (RuntimeException ex) {
            ragRetrievalLogService.markFailed(retrievalLog, ex);
            throw ex;
        }
    }

    private VectorRagCitationResponse toCitation(Document document) {
        Map<String, Object> metadata = new LinkedHashMap<>(document.getMetadata());
        return new VectorRagCitationResponse(
                stringValue(metadata.get("documentId")),
                stringValue(metadata.get("chunkId")),
                stringValue(metadata.get("source")),
                stringValue(metadata.get("businessDomain")),
                stringValue(metadata.get("documentType")),
                stringValue(metadata.get("permissionLevel")),
                document.getScore(),
                document.getText(),
                metadata);
    }

    private String stringValue(Object value) {
        return value == null ? null : value.toString();
    }
}
