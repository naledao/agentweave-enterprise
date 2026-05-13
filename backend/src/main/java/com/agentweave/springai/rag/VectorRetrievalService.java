package com.agentweave.springai.rag;

import com.agentweave.shared.security.CurrentUserService;
import com.agentweave.springai.rag.dto.VectorRagCitationResponse;
import com.agentweave.springai.rag.dto.VectorRagSearchRequest;
import com.agentweave.springai.rag.dto.VectorRagSearchResponse;
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

    public VectorRetrievalService(
            VectorStore vectorStore,
            RagMetadataFilterFactory metadataFilterFactory,
            CurrentUserService currentUserService) {
        this.vectorStore = vectorStore;
        this.metadataFilterFactory = metadataFilterFactory;
        this.currentUserService = currentUserService;
    }

    public VectorRagSearchResponse search(VectorRagSearchRequest request) {
        currentUserService.requireCurrentUser();
        currentUserService.requirePermission(SEARCH_PERMISSION);

        SearchRequest.Builder searchRequest = SearchRequest.builder()
                .query(request.normalizedQuery())
                .topK(request.normalizedTopK())
                .similarityThreshold(request.normalizedSimilarityThreshold());
        metadataFilterFactory.build(request).ifPresent(searchRequest::filterExpression);

        List<VectorRagCitationResponse> citations = vectorStore.similaritySearch(searchRequest.build())
                .stream()
                .map(this::toCitation)
                .toList();

        return new VectorRagSearchResponse(
                request.normalizedQuery(),
                "VECTOR_ONLY",
                request.normalizedTopK(),
                request.normalizedSimilarityThreshold(),
                metadataFilterFactory.describe(request),
                citations);
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
