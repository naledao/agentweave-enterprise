package com.agentweave.graphrag.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.agentweave.graphrag.domain.KnowledgeGraphEntity;
import com.agentweave.graphrag.domain.KnowledgeGraphEntityAlias;
import com.agentweave.graphrag.domain.KnowledgeGraphEntityType;
import com.agentweave.graphrag.domain.KnowledgeGraphRelationship;
import com.agentweave.graphrag.domain.KnowledgeGraphRelationshipType;
import com.agentweave.graphrag.dto.GraphRagRetrievalRequest;
import com.agentweave.graphrag.dto.GraphRagRetrievalResponse;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GraphPathSearchServiceTest {

    private final GraphPathSearchService graphPathSearchService = new GraphPathSearchService(new GraphEntityResolver());

    @Test
    void searchesDeduplicatedGraphPathsAndAggregatesSourceChunkIds() {
        UUID documentId = UUID.randomUUID();
        UUID orderChunkId = UUID.randomUUID();
        UUID paymentChunkId = UUID.randomUUID();
        UUID inventoryChunkId = UUID.randomUUID();
        UUID callChunkId = UUID.randomUUID();
        UUID dependencyChunkId = UUID.randomUUID();

        KnowledgeGraphEntity orderService = entity(
                "Order Processor",
                "order processor",
                KnowledgeGraphEntityType.SERVICE,
                documentId,
                List.of("order processor"),
                List.of(orderChunkId));
        KnowledgeGraphEntity paymentApi = entity(
                "Payment Gateway",
                "payment gateway",
                KnowledgeGraphEntityType.API,
                documentId,
                List.of(),
                List.of(paymentChunkId));
        KnowledgeGraphEntity inventoryService = entity(
                "Inventory Ledger",
                "inventory ledger",
                KnowledgeGraphEntityType.SERVICE,
                documentId,
                List.of(),
                List.of(inventoryChunkId));
        KnowledgeGraphEntityAlias paymentAlias = new KnowledgeGraphEntityAlias(
                UUID.randomUUID(),
                paymentApi.getId(),
                documentId,
                "pay api",
                "pay api");

        KnowledgeGraphRelationship orderToPayment = relationship(
                UUID.randomUUID(),
                documentId,
                orderService.getId(),
                paymentApi.getId(),
                KnowledgeGraphRelationshipType.CALLS,
                0.90d,
                callChunkId);
        KnowledgeGraphRelationship paymentToInventory = relationship(
                UUID.randomUUID(),
                documentId,
                paymentApi.getId(),
                inventoryService.getId(),
                KnowledgeGraphRelationshipType.DEPENDS_ON,
                0.80d,
                dependencyChunkId);

        GraphRagRetrievalResponse response = graphPathSearchService.search(
                new GraphRagRetrievalRequest(
                        "Why does order processor call pay api?",
                        "commerce",
                        "internal",
                        documentId,
                        2,
                        5),
                List.of(orderService, paymentApi, inventoryService),
                List.of(paymentAlias),
                List.of(orderToPayment, paymentToInventory));

        assertThat(response.resolvedEntities()).containsExactly("Order Processor", "Payment Gateway");
        assertThat(response.graphPaths()).hasSize(3);
        assertThat(response.matchedPathCount()).isEqualTo(4);
        assertThat(response.filteredPathCount()).isEqualTo(1);
        assertThat(response.confidenceSummary()).startsWith("count=3");
        assertThat(response.graphPaths().get(0).depth()).isEqualTo(1);
        assertThat(response.graphPaths().get(0).entities()).containsExactly("Order Processor", "Payment Gateway");
        assertThat(response.graphPaths().get(1).depth()).isEqualTo(2);
        assertThat(response.sourceChunkIds())
                .containsExactlyInAnyOrder(
                        orderChunkId.toString(),
                        paymentChunkId.toString(),
                        inventoryChunkId.toString(),
                        callChunkId.toString(),
                        dependencyChunkId.toString());
    }

    @Test
    void returnsEmptyWhenBusinessDomainOrPermissionFiltersExcludeEntities() {
        UUID documentId = UUID.randomUUID();
        KnowledgeGraphEntity orderService = entity(
                "Order Processor",
                "order processor",
                KnowledgeGraphEntityType.SERVICE,
                documentId,
                List.of("order processor"),
                List.of(UUID.randomUUID()));
        KnowledgeGraphEntity paymentApi = entity(
                "Payment Gateway",
                "payment gateway",
                KnowledgeGraphEntityType.API,
                documentId,
                List.of(),
                List.of(UUID.randomUUID()));

        GraphRagRetrievalResponse response = graphPathSearchService.search(
                new GraphRagRetrievalRequest(
                        "Why does order processor call pay api?",
                        "finance",
                        "internal",
                        documentId,
                        2,
                        5),
                List.of(orderService, paymentApi),
                List.of(),
                List.of());

        assertThat(response).isEqualTo(GraphRagRetrievalResponse.empty());
    }

    private KnowledgeGraphEntity entity(
            String name,
            String normalizedName,
            KnowledgeGraphEntityType type,
            UUID sourceDocumentId,
            List<String> aliases,
            List<UUID> chunkIds) {
        return new KnowledgeGraphEntity(
                UUID.randomUUID(),
                sourceDocumentId,
                name,
                normalizedName,
                type,
                name + " description",
                aliases,
                "commerce",
                "internal",
                chunkIds);
    }

    private KnowledgeGraphRelationship relationship(
            UUID id,
            UUID sourceDocumentId,
            UUID sourceEntityId,
            UUID targetEntityId,
            KnowledgeGraphRelationshipType type,
            double confidence,
            UUID sourceChunkId) {
        return new KnowledgeGraphRelationship(
                id,
                sourceDocumentId,
                sourceEntityId,
                targetEntityId,
                type,
                type.name() + " relation",
                confidence,
                sourceChunkId);
    }
}
