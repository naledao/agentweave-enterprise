package com.agentweave.graphrag.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.agentweave.graphrag.dto.GraphRagChunkExtraction;
import com.agentweave.graphrag.dto.GraphRagEntityCandidate;
import com.agentweave.graphrag.dto.GraphRagRelationshipCandidate;
import com.agentweave.knowledge.domain.DocumentEntity;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GraphRagNormalizerTest {

    private final GraphRagEntityNormalizer entityNormalizer = new GraphRagEntityNormalizer();
    private final GraphRagRelationshipNormalizer relationshipNormalizer = new GraphRagRelationshipNormalizer();

    @Test
    void normalizesEntitiesAliasesAndRelationshipsByDocument() {
        UUID documentId = UUID.randomUUID();
        UUID chunk1 = UUID.randomUUID();
        UUID chunk2 = UUID.randomUUID();
        DocumentEntity document = document(documentId);

        List<GraphRagChunkExtraction> chunkExtractions = List.of(
                new GraphRagChunkExtraction(
                        chunk1,
                        List.of(new GraphRagEntityCandidate(
                                "Order Service",
                                "SERVICE",
                                "Handles order orchestration",
                                List.of("Order Service", "OrderService"),
                                0.92)),
                        List.of(new GraphRagRelationshipCandidate(
                                "Order Service",
                                "SERVICE",
                                "Payment API",
                                "API",
                                "CALLS",
                                "Order service calls payment api",
                                0.81))),
                new GraphRagChunkExtraction(
                        chunk2,
                        List.of(new GraphRagEntityCandidate(
                                "Payment API",
                                "API",
                                "Handles payment requests",
                                List.of("Payment API"),
                                0.88)),
                        List.of(new GraphRagRelationshipCandidate(
                                "Order Service",
                                "SERVICE",
                                "Payment API",
                                "API",
                                "CALLS",
                                "Duplicate relation from another chunk",
                                0.65))));

        GraphRagEntityNormalizationResult entityResult = entityNormalizer.normalize(document, chunkExtractions);
        List<com.agentweave.graphrag.domain.KnowledgeGraphRelationship> relationships =
                relationshipNormalizer.normalize(documentId, chunkExtractions, entityResult.entityIdsByKey());

        assertThat(entityResult.entities()).hasSize(2);
        assertThat(entityResult.aliases()).hasSize(3);
        assertThat(entityResult.chunkAssociations()).hasSize(4);
        assertThat(entityResult.entities())
                .extracting(com.agentweave.graphrag.domain.KnowledgeGraphEntity::getNormalizedName)
                .contains("order service", "payment api");
        assertThat(relationships).hasSize(1);
        assertThat(relationships.get(0).getType().name()).isEqualTo("CALLS");
        assertThat(relationships.get(0).getSourceDocumentId()).isEqualTo(documentId);
    }

    private DocumentEntity document(UUID documentId) {
        return new DocumentEntity(
                documentId,
                "runbook.md",
                "text/markdown",
                1024,
                "bucket",
                "object",
                "checksum",
                UUID.randomUUID(),
                "runbook",
                "order",
                "RUNBOOK",
                "INTERNAL",
                Instant.parse("2026-05-13T00:00:00Z"),
                null,
                "ops");
    }
}
