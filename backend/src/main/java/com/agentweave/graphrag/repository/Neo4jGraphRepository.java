package com.agentweave.graphrag.repository;

import com.agentweave.graphrag.domain.KnowledgeGraphChunkAssociation;
import com.agentweave.graphrag.domain.KnowledgeGraphEntity;
import com.agentweave.graphrag.domain.KnowledgeGraphRelationship;
import com.agentweave.knowledge.domain.DocumentChunkEntity;
import com.agentweave.knowledge.domain.DocumentEntity;
import java.util.List;
import java.util.UUID;

public interface Neo4jGraphRepository {

    void deleteByDocumentId(UUID documentId);

    void upsertGraph(
            DocumentEntity document,
            List<DocumentChunkEntity> chunks,
            List<KnowledgeGraphEntity> entities,
            List<KnowledgeGraphRelationship> relationships,
            List<KnowledgeGraphChunkAssociation> chunkAssociations);
}
