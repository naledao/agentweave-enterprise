package com.agentweave.conversation.application;

import com.agentweave.conversation.dto.CitationEventResponse;
import com.agentweave.graphrag.dto.GraphPathResponse;
import com.agentweave.springai.rag.dto.VectorRagCitationResponse;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class RagContextMerger {

    private static final Logger log = LoggerFactory.getLogger(RagContextMerger.class);
    private static final String VECTOR_ONLY = "VECTOR_ONLY";
    private static final String GRAPH_ONLY = "GRAPH_ONLY";
    private static final String HYBRID = "HYBRID";

    public RagPromptContext merge(
            List<VectorRagCitationResponse> vectorCitations,
            List<GraphPathResponse> graphPaths) {
        return merge(null, vectorCitations, graphPaths);
    }

    public RagPromptContext merge(
            String plannedRetrievalMode,
            List<VectorRagCitationResponse> vectorCitations,
            List<GraphPathResponse> graphPaths) {
        long started = System.nanoTime();
        List<CitationEventResponse> citations = deduplicate(vectorCitations).stream()
                .map(this::toCitation)
                .toList();
        List<GraphPathResponse> deduplicatedGraphPaths = deduplicateGraphPaths(graphPaths);
        String retrievalMode = retrievalMode(plannedRetrievalMode, citations, deduplicatedGraphPaths);
        String promptContext = buildPromptContext(citations, deduplicatedGraphPaths);
        log.info(
                "RAG context merge completed: retrievalMode={}, vectorCitationCount={}, graphPathCount={}, sourceChunkCount={}, durationMs={}",
                retrievalMode,
                citations.size(),
                deduplicatedGraphPaths.size(),
                sourceChunkCount(deduplicatedGraphPaths),
                elapsedMillis(started));
        return new RagPromptContext(
                retrievalMode,
                promptContext,
                citations,
                deduplicatedGraphPaths);
    }

    public RagPromptContext mergeVectorCitations(List<VectorRagCitationResponse> vectorCitations) {
        return merge(vectorCitations, List.of());
    }

    private List<VectorRagCitationResponse> deduplicate(List<VectorRagCitationResponse> vectorCitations) {
        Map<String, VectorRagCitationResponse> citations = new LinkedHashMap<>();
        vectorCitations.stream()
                .sorted(Comparator.comparing(
                        citation -> citation.score() == null ? 0.0 : citation.score(),
                        Comparator.reverseOrder()))
                .forEach(citation -> citations.putIfAbsent(deduplicateKey(citation), citation));
        return new ArrayList<>(citations.values());
    }

    private List<GraphPathResponse> deduplicateGraphPaths(List<GraphPathResponse> graphPaths) {
        Map<String, GraphPathResponse> paths = new LinkedHashMap<>();
        graphPaths.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(
                                (GraphPathResponse graphPath) ->
                                        graphPath.confidence() == null ? 0.0 : graphPath.confidence(),
                                Comparator.reverseOrder())
                        .thenComparingInt(GraphPathResponse::depth)
                        .thenComparing(GraphPathResponse::pathId))
                .forEach(graphPath -> paths.putIfAbsent(deduplicateKey(graphPath), graphPath));
        return new ArrayList<>(paths.values());
    }

    private String deduplicateKey(VectorRagCitationResponse citation) {
        if (citation.chunkId() != null && !citation.chunkId().isBlank()) {
            return citation.chunkId();
        }
        if (citation.documentId() != null && !citation.documentId().isBlank()) {
            return citation.documentId() + ":" + citation.snippet();
        }
        return citation.snippet();
    }

    private String deduplicateKey(GraphPathResponse graphPath) {
        if (graphPath.pathId() != null && !graphPath.pathId().isBlank()) {
            return graphPath.pathId();
        }
        return String.join("|", graphPath.entities()) + "::" + String.join("|", graphPath.relationships());
    }

    private CitationEventResponse toCitation(VectorRagCitationResponse citation) {
        String documentName = stringMetadata(citation, "documentName");
        String title = firstPresent(documentName, citation.source(), citation.documentId(), "Knowledge chunk");
        return new CitationEventResponse(
                citation.documentId(),
                documentName,
                citation.chunkId(),
                title,
                citation.source(),
                citation.snippet(),
                citation.score(),
                citation.businessDomain(),
                citation.documentType(),
                citation.permissionLevel());
    }

    private String buildPromptContext(List<CitationEventResponse> citations, List<GraphPathResponse> graphPaths) {
        if (citations.isEmpty() && graphPaths.isEmpty()) {
            return """
                    No relevant knowledge base citations or graph paths were retrieved for this question.
                    If the answer cannot be confirmed from conversation context, say that the current knowledge base has no reliable matching material.
                    """;
        }
        StringBuilder context = new StringBuilder();
        if (!citations.isEmpty()) {
            context.append("Retrieved knowledge citations:\n");
            for (int index = 0; index < citations.size(); index++) {
                CitationEventResponse citation = citations.get(index);
                context.append("\n[")
                        .append(index + 1)
                        .append("] documentId=")
                        .append(valueOrUnknown(citation.documentId()))
                        .append(", chunkId=")
                        .append(valueOrUnknown(citation.chunkId()));
                if (citation.source() != null && !citation.source().isBlank()) {
                    context.append(", source=").append(citation.source());
                }
                if (citation.score() != null) {
                    context.append(", score=").append(citation.score());
                }
                context.append("\n")
                        .append(citation.snippet())
                        .append("\n");
            }
        }
        if (!graphPaths.isEmpty()) {
            if (context.length() > 0) {
                context.append("\n");
            }
            context.append("Retrieved graph paths:\n");
            for (int index = 0; index < graphPaths.size(); index++) {
                GraphPathResponse graphPath = graphPaths.get(index);
                context.append("\n[")
                        .append(index + 1)
                        .append("] pathId=")
                        .append(valueOrUnknown(graphPath.pathId()))
                        .append(", depth=")
                        .append(graphPath.depth());
                if (graphPath.confidence() != null) {
                    context.append(", confidence=").append(graphPath.confidence());
                }
                context.append("\nentities=")
                        .append(String.join(" -> ", graphPath.entities()))
                        .append("\nrelationships=")
                        .append(String.join(" -> ", graphPath.relationships()));
                if (!graphPath.sourceChunkIds().isEmpty()) {
                    context.append("\nsourceChunkIds=")
                            .append(String.join(", ", graphPath.sourceChunkIds()));
                }
                context.append("\n");
            }
        }
        return context.toString();
    }

    private String retrievalMode(
            String plannedRetrievalMode,
            List<CitationEventResponse> citations,
            List<GraphPathResponse> graphPaths) {
        if (plannedRetrievalMode != null && !plannedRetrievalMode.isBlank()) {
            return plannedRetrievalMode;
        }
        boolean hasCitations = !citations.isEmpty();
        boolean hasGraphPaths = !graphPaths.isEmpty();
        if (hasCitations && hasGraphPaths) {
            return HYBRID;
        }
        if (hasGraphPaths) {
            return GRAPH_ONLY;
        }
        return VECTOR_ONLY;
    }

    private String stringMetadata(VectorRagCitationResponse citation, String key) {
        Object value = citation.metadata() == null ? null : citation.metadata().get(key);
        return value == null ? null : value.toString();
    }

    private String firstPresent(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String valueOrUnknown(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }

    private int sourceChunkCount(List<GraphPathResponse> graphPaths) {
        return (int) graphPaths.stream()
                .flatMap(graphPath -> graphPath.sourceChunkIds().stream())
                .distinct()
                .count();
    }

    private long elapsedMillis(long startedNanos) {
        return Math.max(0, (System.nanoTime() - startedNanos) / 1_000_000);
    }
}
