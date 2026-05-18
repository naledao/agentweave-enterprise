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
import com.agentweave.shared.audit.AuditEventType;
import com.agentweave.shared.audit.AuditLog;
import com.agentweave.shared.exception.BusinessException;
import com.agentweave.shared.exception.ErrorCode;
import com.agentweave.shared.exception.ResourceNotFoundException;
import com.agentweave.shared.tracing.CorrelationContext;
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
    private final CorrelationContext correlationContext;

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
            TransactionTemplate transactionTemplate,
            CorrelationContext correlationContext) {
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
        this.correlationContext = correlationContext;
    }

    @AuditLog(
            eventType = AuditEventType.GRAPHRAG_INDEX,
            resourceType = "document",
            resourceId = "#documentId",
            includeResponse = false)
    public GraphRagIndexSummaryResponse index(UUID documentId, String traceId) {
        try (CorrelationContext.Scope ignored = correlationContext.open(traceId, null, null)) {
            DocumentEntity document = findDocument(documentId);
            validate(document);
            List<DocumentChunkEntity> chunks = documentChunkRepository.findByDocumentIdOrderByChunkIndexAsc(documentId);
            if (chunks.isEmpty()) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "document has no indexed chunks to build graph");
            }

            GraphRagIndexLog logEntry = graphRagIndexLogService.start(documentId, traceId, chunks.size());
            int entityCount = 0;
            int relationshipCount = 0;
            int chunkEntityCount = 0;
            try {
                long extractionStarted = System.nanoTime();
                List<GraphRagChunkExtraction> chunkExtractions = graphRagExtractionService.extract(document, chunks);
                int extractedEntityCount = extractedEntityCount(chunkExtractions);
                int extractedRelationshipCount = extractedRelationshipCount(chunkExtractions);
                log.info(
                        "GraphRAG extraction completed: documentId={}, traceId={}, chunkCount={}, entityCandidateCount={}, relationshipCandidateCount={}, durationMs={}",
                        documentId,
                        traceId,
                        chunks.size(),
                        extractedEntityCount,
                        extractedRelationshipCount,
                        elapsedMillis(extractionStarted));

                long normalizationStarted = System.nanoTime();
                GraphRagEntityNormalizationResult entityResult = graphRagEntityNormalizer.normalize(document, chunkExtractions);
                List<KnowledgeGraphRelationship> relationships = graphRagRelationshipNormalizer.normalize(
                        documentId,
                        chunkExtractions,
                        entityResult.entityIdsByKey());
                entityCount = entityResult.entities().size();
                relationshipCount = relationships.size();
                chunkEntityCount = entityResult.chunkAssociations().size();
                log.info(
                        "GraphRAG normalization completed: documentId={}, traceId={}, entityCount={}, aliasCount={}, relationshipCount={}, chunkEntityCount={}, durationMs={}",
                        documentId,
                        traceId,
                        entityCount,
                        entityResult.aliases().size(),
                        relationshipCount,
                        chunkEntityCount,
                        elapsedMillis(normalizationStarted));

                graphRagIndexCleanupService.deleteByDocumentId(documentId, chunkIds(chunks), traceId);

                long postgresStarted = System.nanoTime();
                transactionTemplate.executeWithoutResult(status -> {
                    knowledgeGraphEntityRepository.saveAllAndFlush(entityResult.entities());
                    knowledgeGraphEntityAliasRepository.saveAllAndFlush(entityResult.aliases());
                    knowledgeGraphChunkAssociationRepository.saveAllAndFlush(entityResult.chunkAssociations());
                    knowledgeGraphRelationshipRepository.saveAllAndFlush(relationships);
                });
                log.info(
                        "GraphRAG PostgreSQL graph write completed: documentId={}, traceId={}, entityCount={}, relationshipCount={}, chunkEntityCount={}, durationMs={}",
                        documentId,
                        traceId,
                        entityCount,
                        relationshipCount,
                        chunkEntityCount,
                        elapsedMillis(postgresStarted));

                long neo4jStarted = System.nanoTime();
                neo4jGraphRepository.upsertGraph(
                        document,
                        chunks,
                        entityResult.entities(),
                        relationships,
                        entityResult.chunkAssociations());
                log.info(
                        "GraphRAG Neo4j graph write completed: documentId={}, traceId={}, neo4jEnabled={}, entityCount={}, relationshipCount={}, chunkEntityCount={}, durationMs={}",
                        documentId,
                        traceId,
                        logEntry.isNeo4jEnabled(),
                        entityCount,
                        relationshipCount,
                        chunkEntityCount,
                        elapsedMillis(neo4jStarted));

                graphRagIndexLogService.markCompleted(
                        logEntry,
                        document.getBusinessDomain(),
                        document.getPermissionLevel(),
                        entityCount,
                        relationshipCount,
                        chunks.size(),
                        chunkEntityCount);
                log.info(
                        "GraphRAG index completed: documentId={}, traceId={}, entityCount={}, relationshipCount={}, chunkCount={}, chunkEntityCount={}",
                        documentId,
                        traceId,
                        entityCount,
                        relationshipCount,
                        chunks.size(),
                        chunkEntityCount);
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
                graphRagIndexLogService.markFailed(
                        logEntry,
                        document.getBusinessDomain(),
                        document.getPermissionLevel(),
                        summary,
                        entityCount,
                        relationshipCount,
                        chunks.size(),
                        chunkEntityCount);
                log.warn(
                        "GraphRAG index failed: documentId={}, traceId={}, error={}",
                        documentId,
                        traceId,
                        summary,
                        ex);
            }

            return graphRagIndexLogService.latestSummary(documentId);
        }
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

    private int extractedEntityCount(List<GraphRagChunkExtraction> chunkExtractions) {
        return chunkExtractions.stream()
                .mapToInt(extraction -> extraction.entities().size())
                .sum();
    }

    private int extractedRelationshipCount(List<GraphRagChunkExtraction> chunkExtractions) {
        return chunkExtractions.stream()
                .mapToInt(extraction -> extraction.relationships().size())
                .sum();
    }

    private long elapsedMillis(long startedNanos) {
        return Math.max(0, (System.nanoTime() - startedNanos) / 1_000_000);
    }

    private String errorSummary(Exception ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            message = ex.getClass().getSimpleName();
        }
        return message.length() > 1000 ? message.substring(0, 1000) : message;
    }
}
