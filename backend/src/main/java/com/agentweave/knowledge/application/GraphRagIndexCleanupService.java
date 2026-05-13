package com.agentweave.knowledge.application;

import com.agentweave.graphrag.repository.KnowledgeGraphChunkAssociationRepository;
import com.agentweave.graphrag.repository.KnowledgeGraphEntityAliasRepository;
import com.agentweave.graphrag.repository.KnowledgeGraphEntityRepository;
import com.agentweave.graphrag.repository.KnowledgeGraphRelationshipRepository;
import com.agentweave.graphrag.repository.Neo4jGraphRepository;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class GraphRagIndexCleanupService {

    private static final Logger log = LoggerFactory.getLogger(GraphRagIndexCleanupService.class);

    private final KnowledgeGraphRelationshipRepository knowledgeGraphRelationshipRepository;
    private final KnowledgeGraphChunkAssociationRepository knowledgeGraphChunkAssociationRepository;
    private final KnowledgeGraphEntityAliasRepository knowledgeGraphEntityAliasRepository;
    private final KnowledgeGraphEntityRepository knowledgeGraphEntityRepository;
    private final Neo4jGraphRepository neo4jGraphRepository;
    private final TransactionTemplate transactionTemplate;

    public GraphRagIndexCleanupService(
            KnowledgeGraphRelationshipRepository knowledgeGraphRelationshipRepository,
            KnowledgeGraphChunkAssociationRepository knowledgeGraphChunkAssociationRepository,
            KnowledgeGraphEntityAliasRepository knowledgeGraphEntityAliasRepository,
            KnowledgeGraphEntityRepository knowledgeGraphEntityRepository,
            Neo4jGraphRepository neo4jGraphRepository,
            TransactionTemplate transactionTemplate) {
        this.knowledgeGraphRelationshipRepository = knowledgeGraphRelationshipRepository;
        this.knowledgeGraphChunkAssociationRepository = knowledgeGraphChunkAssociationRepository;
        this.knowledgeGraphEntityAliasRepository = knowledgeGraphEntityAliasRepository;
        this.knowledgeGraphEntityRepository = knowledgeGraphEntityRepository;
        this.neo4jGraphRepository = neo4jGraphRepository;
        this.transactionTemplate = transactionTemplate;
    }

    public void deleteByDocumentId(UUID documentId, List<UUID> chunkIds, String traceId) {
        int chunkCount = chunkIds == null ? 0 : chunkIds.size();
        transactionTemplate.executeWithoutResult(status -> {
            knowledgeGraphRelationshipRepository.deleteBySourceDocumentId(documentId);
            knowledgeGraphChunkAssociationRepository.deleteBySourceDocumentId(documentId);
            knowledgeGraphEntityAliasRepository.deleteBySourceDocumentId(documentId);
            knowledgeGraphEntityRepository.deleteBySourceDocumentId(documentId);
        });
        neo4jGraphRepository.deleteByDocumentId(documentId);
        log.info(
                "GraphRAG index cleanup completed: documentId={}, chunkCount={}, traceId={}",
                documentId,
                chunkCount,
                traceId);
    }
}
