package com.agentweave.conversation.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.agentweave.conversation.domain.ConversationEntity;
import com.agentweave.conversation.domain.ConversationMessageEntity;
import com.agentweave.conversation.domain.MessageRole;
import com.agentweave.conversation.domain.MessageStatus;
import com.agentweave.conversation.dto.CitationEventResponse;
import com.agentweave.conversation.dto.ConversationMessageResponse;
import com.agentweave.graphrag.dto.GraphPathResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MessageMetadataServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MessageMetadataService messageMetadataService = new MessageMetadataService(objectMapper);

    @Test
    void assistantRagMetadataIncludesGraphPathsAndCanBeReadBack() throws Exception {
        CitationEventResponse citation = new CitationEventResponse(
                "doc-1",
                "Runbook doc-1",
                "chunk-1",
                "Runbook doc-1",
                "internal",
                "service status checklist",
                0.93d,
                "order",
                "RUNBOOK",
                "INTERNAL");
        GraphPathResponse graphPath = new GraphPathResponse(
                "path-1",
                2,
                List.of("Order Service", "Payment API"),
                List.of("CALLS"),
                List.of("chunk-1", "chunk-2"),
                0.88d);
        RagPromptContext ragContext = new RagPromptContext("HYBRID", "prompt", List.of(citation), List.of(graphPath));

        String metadata = messageMetadataService.assistantRagMetadata(ragContext);
        JsonNode root = objectMapper.readTree(metadata);

        assertThat(root.path("retrievalMode").asText()).isEqualTo("HYBRID");
        assertThat(root.path("citations").size()).isEqualTo(1);
        assertThat(root.path("citations").get(0).path("businessDomain").asText()).isEqualTo("order");
        assertThat(root.path("citations").get(0).path("documentType").asText()).isEqualTo("RUNBOOK");
        assertThat(root.path("citations").get(0).path("permissionLevel").asText()).isEqualTo("INTERNAL");
        assertThat(root.path("graphPaths").size()).isEqualTo(1);
        assertThat(root.path("graphPaths").get(0).path("pathId").asText()).isEqualTo("path-1");
        assertThat(root.path("graphPaths").get(0).path("entities").get(0).asText()).isEqualTo("Order Service");
        assertThat(root.path("graphPaths").get(0).path("entities").get(1).asText()).isEqualTo("Payment API");

        UUID conversationId = UUID.randomUUID();
        ConversationEntity conversation = new ConversationEntity(conversationId, UUID.randomUUID(), "RAG metadata");
        ConversationMessageEntity message = new ConversationMessageEntity(
                UUID.randomUUID(),
                conversation.getOwnerUserId(),
                MessageRole.ASSISTANT,
                "answer",
                MessageStatus.SUCCEEDED,
                "trace-1");
        conversation.addMessage(message);
        message.replaceMetadata(metadata);

        ConversationMessageResponse response = ConversationMessageResponse.from(message);

        assertThat(response.retrievalMode()).isEqualTo("HYBRID");
        assertThat(response.citations()).containsExactly(citation);
        assertThat(response.graphPaths()).containsExactly(graphPath);
    }
}
