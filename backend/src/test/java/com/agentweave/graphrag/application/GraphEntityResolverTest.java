package com.agentweave.graphrag.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.agentweave.graphrag.domain.KnowledgeGraphEntity;
import com.agentweave.graphrag.domain.KnowledgeGraphEntityAlias;
import com.agentweave.graphrag.domain.KnowledgeGraphEntityType;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GraphEntityResolverTest {

    private final GraphEntityResolver graphEntityResolver = new GraphEntityResolver();

    @Test
    void resolvesEntitiesByNameAndAliasAndPrefersStrongMatches() {
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
        KnowledgeGraphEntityAlias paymentAlias = new KnowledgeGraphEntityAlias(
                UUID.randomUUID(),
                paymentApi.getId(),
                documentId,
                "pay api",
                "pay api");

        List<GraphEntityMatch> matches = graphEntityResolver.resolve(
                "Why does order processor call pay api?",
                List.of(orderService, paymentApi),
                List.of(paymentAlias));

        assertThat(matches).hasSize(2);
        assertThat(matches.get(0).entity().getName()).isEqualTo("Order Processor");
        assertThat(matches.get(0).matchedText()).isEqualTo("Order Processor");
        assertThat(matches.get(1).entity().getName()).isEqualTo("Payment Gateway");
        assertThat(matches.get(1).matchedText()).isEqualTo("pay api");
        assertThat(graphEntityResolver.shouldSearchGraph("Why does order processor call pay api?", matches)).isTrue();
    }

    @Test
    void doesNotSearchGraphWhenOnlyOneStrongMatchAndNoGraphKeywordIsPresent() {
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

        List<GraphEntityMatch> matches = graphEntityResolver.resolve(
                "order processor status",
                List.of(orderService, paymentApi),
                List.of());

        assertThat(matches).hasSize(1);
        assertThat(matches.get(0).entity().getName()).isEqualTo("Order Processor");
        assertThat(graphEntityResolver.shouldSearchGraph("order processor status", matches)).isFalse();
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
}
