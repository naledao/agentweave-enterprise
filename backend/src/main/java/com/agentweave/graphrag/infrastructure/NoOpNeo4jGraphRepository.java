package com.agentweave.graphrag.infrastructure;

import com.agentweave.graphrag.domain.KnowledgeGraphChunkAssociation;
import com.agentweave.graphrag.domain.KnowledgeGraphEntity;
import com.agentweave.graphrag.domain.KnowledgeGraphRelationship;
import com.agentweave.graphrag.repository.Neo4jGraphRepository;
import com.agentweave.knowledge.domain.DocumentChunkEntity;
import com.agentweave.knowledge.domain.DocumentEntity;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "agentweave.graphrag.neo4j", name = "enabled", havingValue = "false", matchIfMissing = true)
public class NoOpNeo4jGraphRepository implements Neo4jGraphRepository {

    private static final Logger log = LoggerFactory.getLogger(NoOpNeo4jGraphRepository.class);

    @Override
    public void deleteByDocumentId(UUID documentId) {
        log.debug("Neo4j graph delete skipped: documentId={}", documentId);
    }

    @Override
    public void upsertGraph(
            DocumentEntity document,
            List<DocumentChunkEntity> chunks,
            List<KnowledgeGraphEntity> entities,
            List<KnowledgeGraphRelationship> relationships,
            List<KnowledgeGraphChunkAssociation> chunkAssociations) {
        log.debug(
                "Neo4j graph write skipped: documentId={}, chunkCount={}, entityCount={}, relationshipCount={}, chunkAssociationCount={}",
                document.getId(),
                chunks == null ? 0 : chunks.size(),
                entities == null ? 0 : entities.size(),
                relationships == null ? 0 : relationships.size(),
                chunkAssociations == null ? 0 : chunkAssociations.size());
    }
}
