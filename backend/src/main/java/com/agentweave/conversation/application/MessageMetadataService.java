package com.agentweave.conversation.application;

import com.agentweave.conversation.dto.CitationEventResponse;
import com.agentweave.graphrag.dto.GraphPathResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class MessageMetadataService {

    private final ObjectMapper objectMapper;

    public MessageMetadataService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String assistantRagMetadata(RagPromptContext ragContext) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("retrievalMode", ragContext.retrievalMode());
        metadata.put("citations", citationMetadata(ragContext.citations()));
        metadata.put("graphPaths", graphPathMetadata(ragContext.graphPaths()));
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("failed to serialize assistant message metadata", ex);
        }
    }

    private List<Map<String, Object>> citationMetadata(List<CitationEventResponse> citations) {
        return citations.stream()
                .map(this::citationMetadata)
                .toList();
    }

    private Map<String, Object> citationMetadata(CitationEventResponse citation) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        putIfPresent(metadata, "documentId", citation.documentId());
        putIfPresent(metadata, "documentName", citation.documentName());
        putIfPresent(metadata, "chunkId", citation.chunkId());
        putIfPresent(metadata, "title", citation.title());
        putIfPresent(metadata, "source", citation.source());
        putIfPresent(metadata, "snippet", citation.snippet());
        putIfPresent(metadata, "score", citation.score());
        return metadata;
    }

    private List<Map<String, Object>> graphPathMetadata(List<GraphPathResponse> graphPaths) {
        return graphPaths.stream()
                .map(this::graphPathMetadata)
                .toList();
    }

    private Map<String, Object> graphPathMetadata(GraphPathResponse graphPath) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        putIfPresent(metadata, "pathId", graphPath.pathId());
        metadata.put("depth", graphPath.depth());
        metadata.put("entities", graphPath.entities());
        metadata.put("relationships", graphPath.relationships());
        metadata.put("sourceChunkIds", graphPath.sourceChunkIds());
        putIfPresent(metadata, "confidence", graphPath.confidence());
        return metadata;
    }

    private void putIfPresent(Map<String, Object> target, String key, Object value) {
        if (value != null) {
            target.put(key, value);
        }
    }
}
