package com.agentweave.graphrag.application;

import com.agentweave.graphrag.domain.KnowledgeGraphRelationship;
import com.agentweave.graphrag.domain.KnowledgeGraphRelationshipType;
import com.agentweave.graphrag.dto.GraphRagChunkExtraction;
import com.agentweave.graphrag.dto.GraphRagRelationshipCandidate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class GraphRagRelationshipNormalizer {

    public List<KnowledgeGraphRelationship> normalize(
            UUID documentId,
            List<GraphRagChunkExtraction> chunkExtractions,
            Map<GraphRagEntityKey, UUID> entityIdsByKey) {
        Map<RelationshipKey, KnowledgeGraphRelationship> relationships = new LinkedHashMap<>();
        for (GraphRagChunkExtraction chunkExtraction : chunkExtractions) {
            for (GraphRagRelationshipCandidate candidate : chunkExtraction.relationships()) {
                GraphRagEntityKey sourceKey = new GraphRagEntityKey(
                        normalizeText(candidate.sourceName()),
                        com.agentweave.graphrag.domain.KnowledgeGraphEntityType.from(candidate.sourceType()));
                GraphRagEntityKey targetKey = new GraphRagEntityKey(
                        normalizeText(candidate.targetName()),
                        com.agentweave.graphrag.domain.KnowledgeGraphEntityType.from(candidate.targetType()));
                UUID sourceEntityId = entityIdsByKey.get(sourceKey);
                UUID targetEntityId = entityIdsByKey.get(targetKey);
                if (sourceEntityId == null || targetEntityId == null) {
                    continue;
                }
                KnowledgeGraphRelationshipType type = KnowledgeGraphRelationshipType.from(candidate.type());
                RelationshipKey key = new RelationshipKey(
                        sourceEntityId,
                        targetEntityId,
                        type);
                relationships.computeIfAbsent(key, ignored -> new KnowledgeGraphRelationship(
                        UUID.randomUUID(),
                        documentId,
                        sourceEntityId,
                        targetEntityId,
                        type,
                        candidate.description(),
                        confidence(candidate.confidence()),
                        chunkExtraction.chunkId()));
            }
        }
        return new ArrayList<>(relationships.values());
    }

    private String normalizeText(String value) {
        if (value == null) {
            return "";
        }
        return value.trim()
                .toLowerCase(java.util.Locale.ROOT)
                .replaceAll("[\\p{Punct}\\s]+", " ");
    }

    private double confidence(double confidence) {
        return Math.max(0.0d, Math.min(1.0d, confidence));
    }

    private record RelationshipKey(
            UUID sourceEntityId,
            UUID targetEntityId,
            KnowledgeGraphRelationshipType type) {
    }
}
