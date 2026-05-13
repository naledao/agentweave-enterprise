package com.agentweave.knowledge.application;

import com.agentweave.knowledge.domain.DocumentChunkEntity;
import com.agentweave.knowledge.domain.DocumentStatus;
import com.agentweave.knowledge.domain.DocumentEntity;
import com.agentweave.knowledge.repository.DocumentChunkRepository;
import com.agentweave.knowledge.repository.DocumentRepository;
import com.agentweave.shared.exception.BusinessException;
import com.agentweave.shared.exception.ErrorCode;
import com.agentweave.shared.exception.ResourceNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class DocumentReindexPipelineService {

    private static final Logger log = LoggerFactory.getLogger(DocumentReindexPipelineService.class);

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final DocumentMetadataFactory documentMetadataFactory;
    private final DocumentChunkingService documentChunkingService;
    private final GraphRagIndexCleanupService graphRagIndexCleanupService;
    private final PgVectorIndexService pgVectorIndexService;
    private final TransactionTemplate transactionTemplate;

    public DocumentReindexPipelineService(
            DocumentRepository documentRepository,
            DocumentChunkRepository documentChunkRepository,
            DocumentMetadataFactory documentMetadataFactory,
            DocumentChunkingService documentChunkingService,
            GraphRagIndexCleanupService graphRagIndexCleanupService,
            PgVectorIndexService pgVectorIndexService,
            TransactionTemplate transactionTemplate) {
        this.documentRepository = documentRepository;
        this.documentChunkRepository = documentChunkRepository;
        this.documentMetadataFactory = documentMetadataFactory;
        this.documentChunkingService = documentChunkingService;
        this.graphRagIndexCleanupService = graphRagIndexCleanupService;
        this.pgVectorIndexService = pgVectorIndexService;
        this.transactionTemplate = transactionTemplate;
    }

    public DocumentChunkingResult prepareReindex(UUID documentId, String traceId) {
        ReindexPreparation preparation = prepareDocument(documentId, traceId);
        if (preparation.alreadyPrepared()) {
            log.info("Document reindex already prepared, republishing chunked event: documentId={}, traceId={}",
                    documentId,
                    traceId);
            return new DocumentChunkingResult(preparation.document(), preparation.chunkCount());
        }

        try {
            graphRagIndexCleanupService.deleteByDocumentId(documentId, preparation.oldChunkIds(), traceId);
            pgVectorIndexService.deleteByDocumentId(documentId);
            DocumentChunkingResult result = transactionTemplate.execute(status -> {
                DocumentEntity document = findDocumentWithLock(documentId);
                List<String> chunkContents = documentChunkingService.split(document.getCleanedText());
                documentChunkRepository.deleteByDocumentId(documentId);
                List<DocumentChunkEntity> chunks = createChunks(document, chunkContents);
                documentChunkRepository.saveAllAndFlush(chunks);
                document.markEmbedding(traceId);
                DocumentEntity savedDocument = documentRepository.saveAndFlush(document);
                return new DocumentChunkingResult(savedDocument, chunks.size());
            });
            log.info("Document reindex prepared: documentId={}, traceId={}, chunkCount={}",
                    documentId,
                    traceId,
                    result.chunkCount());
            return result;
        } catch (RuntimeException ex) {
            String summary = errorSummary(ex);
            log.warn("Document reindex preparation failed: documentId={}, traceId={}, error={}",
                    documentId,
                    traceId,
                    summary,
                    ex);
            markFailed(documentId, summary, traceId);
            if (ex instanceof BusinessException businessException) {
                throw businessException;
            }
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, summary);
        }
    }

    private ReindexPreparation prepareDocument(UUID documentId, String traceId) {
        return transactionTemplate.execute(status -> {
            DocumentEntity document = findDocumentWithLock(documentId);
            if (document.getStatus() == DocumentStatus.EMBEDDING
                    && Objects.equals(traceId, document.getTraceId())
                    && documentChunkRepository.countByDocumentId(documentId) > 0) {
                int chunkCount = Math.toIntExact(documentChunkRepository.countByDocumentId(documentId));
                return ReindexPreparation.alreadyPrepared(document, chunkCount);
            }
            if (isProcessing(document.getStatus())) {
                throw new BusinessException(
                        ErrorCode.VALIDATION_FAILED,
                        "document is already being processed");
            }
            String cleanedText = document.getCleanedText();
            if (cleanedText == null || cleanedText.isBlank()) {
                throw new BusinessException(
                        ErrorCode.VALIDATION_FAILED,
                        "document must be parsed before reindexing");
            }
            List<UUID> oldChunkIds = documentChunkRepository.findByDocumentIdOrderByChunkIndexAsc(documentId).stream()
                    .map(DocumentChunkEntity::getId)
                    .toList();
            document.markChunking(traceId);
            DocumentEntity savedDocument = documentRepository.saveAndFlush(document);
            return ReindexPreparation.pending(savedDocument, oldChunkIds);
        });
    }

    private boolean isProcessing(DocumentStatus status) {
        return status == DocumentStatus.PARSING
                || status == DocumentStatus.CLEANING
                || status == DocumentStatus.CHUNKING
                || status == DocumentStatus.EMBEDDING;
    }

    private List<DocumentChunkEntity> createChunks(DocumentEntity document, List<String> chunkContents) {
        List<String> normalizedChunks = normalizeChunkContents(chunkContents);
        List<DocumentChunkEntity> chunks = new ArrayList<>();
        for (int index = 0; index < normalizedChunks.size(); index++) {
            UUID chunkId = UUID.randomUUID();
            DocumentMetadata metadata = documentMetadataFactory.forChunk(document, chunkId);
            chunks.add(new DocumentChunkEntity(
                    chunkId,
                    document.getId(),
                    index,
                    normalizedChunks.get(index),
                    metadata.toMap()));
        }
        return chunks;
    }

    private List<String> normalizeChunkContents(List<String> chunkContents) {
        if (chunkContents == null || chunkContents.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "chunk contents must not be empty");
        }
        List<String> normalizedChunks = chunkContents.stream()
                .filter(content -> content != null && !content.isBlank())
                .map(String::trim)
                .toList();
        if (normalizedChunks.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "chunk contents must not be blank");
        }
        return normalizedChunks;
    }

    private void markFailed(UUID documentId, String summary, String traceId) {
        transactionTemplate.executeWithoutResult(status -> documentRepository.findById(documentId)
                .ifPresent(document -> {
                    document.markFailed(summary, traceId);
                    documentRepository.saveAndFlush(document);
                }));
    }

    private DocumentEntity findDocumentWithLock(UUID documentId) {
        return documentRepository.findWithLockById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("document not found"));
    }

    private String errorSummary(Exception ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            message = ex.getClass().getSimpleName();
        }
        return message.length() > 1000 ? message.substring(0, 1000) : message;
    }

    private record ReindexPreparation(
            DocumentEntity document,
            List<UUID> oldChunkIds,
            int chunkCount,
            boolean alreadyPrepared) {

        private static ReindexPreparation pending(DocumentEntity document, List<UUID> oldChunkIds) {
            return new ReindexPreparation(document, oldChunkIds, 0, false);
        }

        private static ReindexPreparation alreadyPrepared(DocumentEntity document, int chunkCount) {
            return new ReindexPreparation(document, List.of(), chunkCount, true);
        }
    }
}
