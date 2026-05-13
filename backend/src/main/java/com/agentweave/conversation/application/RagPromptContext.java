package com.agentweave.conversation.application;

import com.agentweave.conversation.dto.CitationEventResponse;
import com.agentweave.graphrag.dto.GraphPathResponse;
import java.util.List;

public record RagPromptContext(
        String retrievalMode,
        String promptContext,
        List<CitationEventResponse> citations,
        List<GraphPathResponse> graphPaths) {

    private static final RagPromptContext EMPTY = new RagPromptContext("VECTOR_ONLY", "", List.of(), List.of());

    public RagPromptContext {
        retrievalMode = retrievalMode == null || retrievalMode.isBlank() ? "VECTOR_ONLY" : retrievalMode;
        promptContext = promptContext == null ? "" : promptContext;
        citations = citations == null ? List.of() : List.copyOf(citations);
        graphPaths = graphPaths == null ? List.of() : List.copyOf(graphPaths);
    }

    public static RagPromptContext empty() {
        return EMPTY;
    }

    public boolean hasCitations() {
        return !citations.isEmpty();
    }

    public boolean hasGraphPaths() {
        return !graphPaths.isEmpty();
    }
}
