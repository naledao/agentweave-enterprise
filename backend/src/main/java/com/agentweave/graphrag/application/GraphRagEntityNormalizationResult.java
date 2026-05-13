package com.agentweave.graphrag.application;

import com.agentweave.graphrag.domain.KnowledgeGraphChunkAssociation;
import com.agentweave.graphrag.domain.KnowledgeGraphEntity;
import com.agentweave.graphrag.domain.KnowledgeGraphEntityAlias;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record GraphRagEntityNormalizationResult(
        List<KnowledgeGraphEntity> entities,
        List<KnowledgeGraphEntityAlias> aliases,
        List<KnowledgeGraphChunkAssociation> chunkAssociations,
        Map<GraphRagEntityKey, UUID> entityIdsByKey) {

    public GraphRagEntityNormalizationResult {
        entities = entities == null ? List.of() : List.copyOf(entities);
        aliases = aliases == null ? List.of() : List.copyOf(aliases);
        chunkAssociations = chunkAssociations == null ? List.of() : List.copyOf(chunkAssociations);
        entityIdsByKey = entityIdsByKey == null ? Map.of() : Map.copyOf(entityIdsByKey);
    }
}
