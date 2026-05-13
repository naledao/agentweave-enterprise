package com.agentweave.conversation.dto;

import com.agentweave.conversation.domain.ConversationMessageEntity;
import com.agentweave.conversation.domain.MessageRole;
import com.agentweave.graphrag.dto.GraphPathResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record ConversationMessageResponse(
        UUID id,
        UUID conversationId,
        MessageRole role,
        String content,
        ConversationMessageStatusResponse status,
        String errorCode,
        String errorMessage,
        String metadata,
        String retrievalMode,
        String traceId,
        List<CitationEventResponse> citations,
        List<GraphPathResponse> graphPaths,
        List<ToolCallFinishedResponse> toolCalls,
        Instant createdAt) {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static ConversationMessageResponse from(ConversationMessageEntity message) {
        return new ConversationMessageResponse(
                message.getId(),
                message.getConversation().getId(),
                message.getRole(),
                message.getContent(),
                ConversationMessageStatusResponse.from(message.getStatus()),
                message.getErrorCode(),
                message.getErrorMessage(),
                message.getMetadata(),
                retrievalModeFromMetadata(message.getMetadata()),
                message.getTraceId(),
                citationsFromMetadata(message.getMetadata()),
                graphPathsFromMetadata(message.getMetadata()),
                List.of(),
                message.getCreatedAt());
    }

    private static List<CitationEventResponse> citationsFromMetadata(String metadata) {
        if (metadata == null || metadata.isBlank()) {
            return List.of();
        }
        try {
            JsonNode citations = OBJECT_MAPPER.readTree(metadata).path("citations");
            if (!citations.isArray()) {
                return List.of();
            }
            List<CitationEventResponse> responses = new ArrayList<>();
            for (JsonNode citation : citations) {
                responses.add(new CitationEventResponse(
                        text(citation, "documentId"),
                        text(citation, "documentName"),
                        text(citation, "chunkId"),
                        text(citation, "title"),
                        text(citation, "source"),
                        text(citation, "snippet"),
                        citation.hasNonNull("score") ? citation.get("score").asDouble() : null,
                        text(citation, "businessDomain"),
                        text(citation, "documentType"),
                        text(citation, "permissionLevel")));
            }
            return responses;
        } catch (Exception ex) {
            return List.of();
        }
    }

    private static String retrievalModeFromMetadata(String metadata) {
        if (metadata == null || metadata.isBlank()) {
            return null;
        }
        try {
            return text(OBJECT_MAPPER.readTree(metadata), "retrievalMode");
        } catch (Exception ex) {
            return null;
        }
    }

    private static List<GraphPathResponse> graphPathsFromMetadata(String metadata) {
        if (metadata == null || metadata.isBlank()) {
            return List.of();
        }
        try {
            JsonNode graphPaths = OBJECT_MAPPER.readTree(metadata).path("graphPaths");
            if (!graphPaths.isArray()) {
                return List.of();
            }
            List<GraphPathResponse> responses = new ArrayList<>();
            for (JsonNode graphPath : graphPaths) {
                responses.add(new GraphPathResponse(
                        text(graphPath, "pathId"),
                        graphPath.hasNonNull("depth") ? graphPath.get("depth").asInt() : 0,
                        strings(graphPath, "entities"),
                        strings(graphPath, "relationships"),
                        strings(graphPath, "sourceChunkIds"),
                        graphPath.hasNonNull("confidence") ? graphPath.get("confidence").asDouble() : null));
            }
            return responses;
        } catch (Exception ex) {
            return List.of();
        }
    }

    private static List<String> strings(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (!value.isArray()) {
            return List.of();
        }
        List<String> results = new ArrayList<>();
        for (JsonNode item : value) {
            if (!item.isNull()) {
                results.add(item.asText());
            }
        }
        return results;
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? null : value.asText();
    }
}
