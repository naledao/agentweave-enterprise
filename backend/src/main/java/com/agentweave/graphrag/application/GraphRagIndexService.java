package com.agentweave.graphrag.application;

import com.agentweave.graphrag.domain.GraphRagIndexLog;
import com.agentweave.graphrag.domain.KnowledgeGraphChunkAssociation;
import com.agentweave.graphrag.domain.KnowledgeGraphEntity;
import com.agentweave.graphrag.domain.KnowledgeGraphEntityAlias;
import com.agentweave.graphrag.domain.KnowledgeGraphRelationship;
import com.agentweave.graphrag.dto.GraphRagChunkExtraction;
import com.agentweave.graphrag.dto.GraphRagIndexSummaryResponse;
import com.agentweave.graphrag.repository.KnowledgeGraphChunkAssociationRepository;
import com.agentweave.graphrag.repository.KnowledgeGraphEntityAliasRepository;
import com.agentweave.graphrag.repository.KnowledgeGraphEntityRepository;
import com.agentweave.graphrag.repository.KnowledgeGraphRelationshipRepository;
import com.agentweave.graphrag.repository.Neo4jGraphRepository;
import com.agentweave.knowledge.application.GraphRagIndexCleanupService;
import com.agentweave.knowledge.domain.DocumentChunkEntity;
import com.agentweave.knowledge.domain.DocumentEntity;
import com.agentweave.knowledge.domain.DocumentStatus;
import com.agentweave.knowledge.repository.DocumentChunkRepository;
import com.agentweave.knowledge.repository.DocumentRepository;
import com.agentweave.shared.exception.BusinessException;
import com.agentweave.shared.exception.ErrorCode;
import com.agentweave.shared.exception.ResourceNotFoundException;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class GraphRagIndexService {

    private static final Logger log = LoggerFactory.getLogger(GraphRagIndexService.class);

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final GraphRagExtractionService graphRagExtractionService;
    private final GraphRagEntityNormalizer graphRagEntityNormalizer;
    private final GraphRagRelationshipNormalizer graphRagRelationshipNormalizer;
    private final GraphRagIndexLogService graphRagIndexLogService;
    private final GraphRagIndexCleanupService graphRagIndexCleanupService;
    private final KnowledgeGraphEntityRepository knowledgeGraphEntityRepository;
    private final KnowledgeGraphEntityAliasRepository knowledgeGraphEntityAliasRepository;
    private final KnowledgeGraphChunkAssociationRepository knowledgeGraphChunkAssociationRepository;
    private final KnowledgeGraphRelationshipRepository knowledgeGraphRelationshipRepository;
    private final Neo4jGraphRepository neo4jGraphRepository;
    private final TransactionTemplate transactionTemplate;

    public GraphRagIndexService(
            DocumentRepository documentRepository,
            DocumentChunkRepository documentChunkRepository,
            GraphRagExtractionService graphRagExtractionService,
            GraphRagEntityNormalizer graphRagEntityNormalizer,
            GraphRagRelationshipNormalizer graphRagRelationshipNormalizer,
            GraphRagIndexLogService graphRagIndexLogService,
            GraphRagIndexCleanupService graphRagIndexCleanupService,
            KnowledgeGraphEntityRepository knowledgeGraphEntityRepository,
            KnowledgeGraphEntityAliasRepository knowledgeGraphEntityAliasRepository,
            KnowledgeGraphChunkAssociationRepository knowledgeGraphChunkAssociationRepository,
            KnowledgeGraphRelationshipRepository knowledgeGraphRelationshipRepository,
            Neo4jGraphRepository neo4jGraphRepository,
            TransactionTemplate transactionTemplate) {
        this.documentRepository = documentRepository;
        this.documentChunkRepository = documentChunkRepository;
        this.graphRagExtractionService = graphRagExtractionService;
        this.graphRagEntityNormalizer = graphRagEntityNormalizer;
        this.graphRagRelationshipNormalizer = graphRagRelationshipNormalizer;
        this.graphRagIndexLogService = graphRagIndexLogService;
        this.graphRagIndexCleanupService = graphRagIndexCleanupService;
        this.knowledgeGraphEntityRepository = knowledgeGraphEntityRepository;
        this.knowledgeGraphEntityAliasRepository = knowledgeGraphEntityAliasRepository;
        this.knowledgeGraphChunkAssociationRepository = knowledgeGraphChunkAssociationRepository;
        this.knowledgeGraphRelationshipRepository = knowledgeGraphRelationshipRepository;
        this.neo4jGraphRepository = neo4jGraphRepository;
        this.transactionTemplate = transactionTemplate;
    }

    public GraphRagIndexSummaryResponse index(UUID documentId, String traceId) {
        DocumentEntity document = findDocument(documentId);
        validate(document);
        List<DocumentChunkEntity> chunks = documentChunkRepository.findByDocumentIdOrderByChunkIndexAsc(documentId);
        if (chunks.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "document has no indexed chunks to build graph");
        }

        GraphRagIndexLog logEntry = graphRagIndexLogService.start(documentId, traceId, chunks.size());
        int entityCount = 0;
        int relationshipCount = 0;
        try {
            List<GraphRagChunkExtraction> chunkExtractions = graphRagExtractionService.extract(document, chunks);
            GraphRagEntityNormalizationResult entityResult = graphRagEntityNormalizer.normalize(document, chunkExtractions);
            List<KnowledgeGraphRelationship> relationships = graphRagRelationshipNormalizer.normalize(
                    documentId,
                    chunkExtractions,
                    entityResult.entityIdsByKey());

            graphRagIndexCleanupService.deleteByDocumentId(documentId, chunkIds(chunks), traceId);

            transactionTemplate.executeWithoutResult(status -> {
                knowledgeGraphEntityRepository.saveAllAndFlush(entityResult.entities());
                knowledgeGraphEntityAliasRepository.saveAllAndFlush(entityResult.aliases());
                knowledgeGraphChunkAssociationRepository.saveAllAndFlush(entityResult.chunkAssociations());
                knowledgeGraphRelationshipRepository.saveAllAndFlush(relationships);
            });

            neo4jGraphRepository.upsertGraph(
                    document,
                    chunks,
                    entityResult.entities(),
                    relationships,
                    entityResult.chunkAssociations());

            entityCount = entityResult.entities().size();
            relationshipCount = relationships.size();
            graphRagIndexLogService.markCompleted(logEntry, entityCount, relationshipCount, chunks.size());
            log.info(
                    "GraphRAG index completed: documentId={}, traceId={}, entityCount={}, relationshipCount={}, chunkCount={}",
                    documentId,
                    traceId,
                    entityCount,
                    relationshipCount,
                    chunks.size());
        } catch (RuntimeException ex) {
            String summary = errorSummary(ex);
            try {
                graphRagIndexCleanupService.deleteByDocumentId(documentId, chunkIds(chunks), traceId);
            } catch (RuntimeException cleanupEx) {
                log.warn(
                        "GraphRAG cleanup after failure also failed: documentId={}, traceId={}, error={}",
                        documentId,
                        traceId,
                        errorSummary(cleanupEx),
                        cleanupEx);
            }
            graphRagIndexLogService.markFailed(logEntry, summary, entityCount, relationshipCount, chunks.size());
            log.warn(
                    "GraphRAG index failed: documentId={}, traceId={}, error={}",
                    documentId,
                    traceId,
                    summary,
                    ex);
        }

        return graphRagIndexLogService.latestSummary(documentId);
    }

    private DocumentEntity findDocument(UUID documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("document not found"));
    }

    private void validate(DocumentEntity document) {
        if (document.getStatus() != DocumentStatus.INDEXED) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "document must be indexed before graph indexing");
        }
    }

    private List<UUID> chunkIds(List<DocumentChunkEntity> chunks) {
        return chunks.stream().map(DocumentChunkEntity::getId).toList();
    }

    private String errorSummary(Exception ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            message = ex.getClass().getSimpleName();
        }
        return message.length() > 1000 ? message.substring(0, 1000) : message;
    }
}
