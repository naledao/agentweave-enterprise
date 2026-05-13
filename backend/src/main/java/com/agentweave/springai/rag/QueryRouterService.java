package com.agentweave.springai.rag;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class QueryRouterService {

    private static final int DEFAULT_VECTOR_TOP_K = 5;
    private static final double DEFAULT_SIMILARITY_THRESHOLD = 0.0;
    private static final int DEFAULT_GRAPH_MAX_DEPTH = 2;

    private static final List<String> GRAPH_ONLY_HINTS = List.of(
            "relationship path",
            "graph path",
            "graph only",
            "only graph",
            "\u5173\u7cfb\u8def\u5f84",
            "\u56fe\u8c31\u8def\u5f84",
            "\u4ec5\u56fe\u8c31",
            "\u53ea\u770b\u56fe\u8c31");

    private static final List<String> HYBRID_HINTS = List.of(
            "dependency",
            "depends on",
            "upstream",
            "downstream",
            "owner",
            "call chain",
            "impact scope",
            "root cause",
            "why",
            "caused by",
            "relationship",
            "path",
            "\u4f9d\u8d56",
            "\u8c03\u7528",
            "\u4e0a\u4e0b\u6e38",
            "\u8d1f\u8d23\u4eba",
            "\u5f71\u54cd\u8303\u56f4",
            "\u4e3a\u4ec0\u4e48",
            "\u5bfc\u81f4",
            "\u5173\u8054",
            "\u94fe\u8def",
            "\u8def\u5f84",
            "\u591a\u8df3",
            "\u5f52\u56e0",
            "\u6839\u56e0");

    public RagRoutingDecision route(RagRoutingRequest request) {
        RagRetrievalMode retrievalMode;
        String routingReason;

        if (request.requestedRetrievalMode() != null) {
            retrievalMode = request.requestedRetrievalMode();
            routingReason = "explicit retrieval mode requested";
        } else if (matchesAny(request.normalizedQuery(), GRAPH_ONLY_HINTS)) {
            retrievalMode = RagRetrievalMode.GRAPH_ONLY;
            routingReason = "query asks for relationship or graph path";
        } else if (matchesAny(request.normalizedQuery(), HYBRID_HINTS)) {
            retrievalMode = RagRetrievalMode.HYBRID;
            routingReason = "query contains relationship, dependency, impact, or root-cause hints";
        } else {
            retrievalMode = RagRetrievalMode.VECTOR_ONLY;
            routingReason = "default vector retrieval for factual question";
        }

        RagRetrievalPlan plan = new RagRetrievalPlan(
                retrievalMode,
                retrievalMode.usesVectorRetrieval() ? DEFAULT_VECTOR_TOP_K : 0,
                retrievalMode.usesVectorRetrieval() ? DEFAULT_SIMILARITY_THRESHOLD : 0.0,
                retrievalMode.usesGraphRetrieval() ? DEFAULT_GRAPH_MAX_DEPTH : 0,
                metadataFilter(request),
                graphFilter(request),
                routingReason);
        return new RagRoutingDecision(retrievalMode, plan, routingReason);
    }

    private boolean matchesAny(String query, List<String> hints) {
        if (query == null || query.isBlank()) {
            return false;
        }
        String normalizedQuery = query.toLowerCase(Locale.ROOT);
        return hints.stream().anyMatch(normalizedQuery::contains);
    }

    private Map<String, Object> metadataFilter(RagRoutingRequest request) {
        Map<String, Object> filter = new LinkedHashMap<>();
        putIfPresent(filter, "businessDomain", request.normalizedBusinessDomain());
        putIfPresent(filter, "documentType", request.normalizedDocumentType());
        putIfPresent(filter, "permissionLevel", request.normalizedPermissionLevel());
        return filter;
    }

    private Map<String, Object> graphFilter(RagRoutingRequest request) {
        Map<String, Object> filter = new LinkedHashMap<>();
        putIfPresent(filter, "businessDomain", request.normalizedBusinessDomain());
        putIfPresent(filter, "permissionLevel", request.normalizedPermissionLevel());
        return filter;
    }

    private void putIfPresent(Map<String, Object> filter, String key, Object value) {
        if (value != null) {
            filter.put(key, value);
        }
    }
}
