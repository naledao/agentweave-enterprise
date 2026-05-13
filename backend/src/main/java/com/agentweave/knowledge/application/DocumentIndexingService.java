package com.agentweave.knowledge.application;

import com.agentweave.knowledge.domain.DocumentChunkEntity;
import com.agentweave.knowledge.domain.DocumentChunkStatus;
import com.agentweave.knowledge.domain.DocumentEntity;
import com.agentweave.knowledge.domain.DocumentStatus;
import com.agentweave.knowledge.dto.DocumentResponse;
import com.agentweave.knowledge.messaging.publisher.DocumentReindexRequestedEventPublisher;
import com.agentweave.knowledge.repository.DocumentChunkRepository;
import com.agentweave.knowledge.repository.DocumentRepository;
import com.agentweave.shared.exception.BusinessException;
import com.agentweave.shared.exception.ErrorCode;
import com.agentweave.shared.exception.ResourceNotFoundException;
import com.agentweave.shared.security.CurrentUser;
import com.agentweave.shared.security.CurrentUserService;
import com.agentweave.shared.tracing.TraceIdProvider;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class DocumentIndexingService {

    private static final Logger log = LoggerFactory.getLogger(DocumentIndexingService.class);
    private static final String INDEX_PERMISSION = "knowledge:document:index";

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final PgVectorIndexService pgVectorIndexService;
    private final GraphRagIndexingScheduler graphRagIndexingScheduler;
    private final CurrentUserService currentUserService;
    private final TraceIdProvider traceIdProvider;
    private final TransactionTemplate transactionTemplate;
    private final ObjectProvider<DocumentReindexRequestedEventPublisher> reindexRequestedEventPublisherProvider;

    public DocumentIndexingService(
            DocumentRepository documentRepository,
            DocumentChunkRepository documentChunkRepository,
            PgVectorIndexService pgVectorIndexService,
            GraphRagIndexingScheduler graphRagIndexingScheduler,
            CurrentUserService currentUserService,
            TraceIdProvider traceIdProvider,
            TransactionTemplate transactionTemplate,
            ObjectProvider<DocumentReindexRequestedEventPublisher> reindexRequestedEventPublisherProvider) {
        this.documentRepository = documentRepository;
        this.documentChunkRepository = documentChunkRepository;
        this.pgVectorIndexService = pgVectorIndexService;
        this.graphRagIndexingScheduler = graphRagIndexingScheduler;
        this.currentUserService = currentUserService;
        this.traceIdProvider = traceIdProvider;
        this.transactionTemplate = transactionTemplate;
        this.reindexRequestedEventPublisherProvider = reindexRequestedEventPublisherProvider;
    }

    public DocumentResponse indexDocument(UUID documentId) {
        return indexDocument(documentId, false);
    }

    private DocumentResponse indexDocument(UUID documentId, boolean reindex) {
        currentUserService.requireCurrentUser();
        currentUserService.requirePermission(INDEX_PERMISSION);
        String traceId = traceIdProvider.currentTraceId();

        DocumentVectorIndexingResult result = indexChunkedDocument(documentId, traceId, reindex);
        graphRagIndexingScheduler.enqueue(documentId, traceId);
        return DocumentResponse.from(result.document(), result.chunkCount());
    }

    public DocumentVectorIndexingResult indexChunkedDocument(UUID documentId, String traceId) {
        return indexChunkedDocument(documentId, traceId, false);
    }

    public DocumentVectorIndexingResult indexRebuiltDocument(UUID documentId, String traceId) {
        return indexChunkedDocument(documentId, traceId, true);
    }

    private DocumentVectorIndexingResult indexChunkedDocument(UUID documentId, String traceId, boolean reindex) {
        DocumentIndexingPreparation preparation = prepareDocumentAndChunksForIndexing(documentId, traceId);
        if (preparation.alreadyIndexed()) {
            log.info("Document already vector indexed, skipping vector write: documentId={}, traceId={}",
                    documentId,
                    traceId);
            return new DocumentVectorIndexingResult(preparation.document(), preparation.chunkCount());
        }

        List<DocumentChunkEntity> chunks = preparation.chunks();
        try {
            pgVectorIndexService.index(chunks);
            DocumentEntity indexedDocument = transactionTemplate.execute(status -> {
                List<DocumentChunkEntity> persistedChunks =
                        documentChunkRepository.findByDocumentIdOrderByChunkIndexAsc(documentId);
                persistedChunks.forEach(chunk -> chunk.markIndexed(chunk.getId(), traceId));
                documentChunkRepository.saveAllAndFlush(persistedChunks);

                DocumentEntity document = findDocument(documentId);
                if (reindex) {
                    document.markReindexed(traceId);
                } else {
                    document.markIndexed(traceId);
                }
                return documentRepository.saveAndFlush(document);
            });
            return new DocumentVectorIndexingResult(indexedDocument, chunks.size());
        } catch (RuntimeException ex) {
            String summary = errorSummary(ex);
            log.warn("Document vector indexing failed: documentId={}, traceId={}, error={}",
                    documentId,
                    traceId,
                    summary,
                    ex);
            markFailed(documentId, chunks, summary, traceId);
            if (ex instanceof BusinessException businessException) {
                throw businessException;
            }
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, summary);
        }
    }

    public DocumentResponse reindexDocument(UUID documentId) {
        CurrentUser user = currentUserService.requireCurrentUser();
        currentUserService.requirePermission(INDEX_PERMISSION);
        String traceId = traceIdProvider.currentTraceId();
        DocumentEntity document = findDocument(documentId);
        DocumentReindexRequestedEventPublisher publisher = reindexRequestedEventPublisherProvider.getIfAvailable();
        if (publisher == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "document reindex queue is disabled");
        }

        try {
            publisher.publish(document, traceId, user.id());
            log.info("Document reindex requested: documentId={}, traceId={}, triggeredBy={}",
                    documentId,
                    traceId,
                    user.id());
            long chunkCount = documentChunkRepository.countByDocumentId(documentId);
            return DocumentResponse.from(document, chunkCount);
        } catch (RuntimeException ex) {
            String summary = errorSummary(ex);
            log.warn("Document reindex request failed: documentId={}, traceId={}, error={}",
                    documentId,
                    traceId,
                    summary,
                    ex);
            if (ex instanceof BusinessException businessException) {
                throw businessException;
            }
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, summary);
        }
    }

    private DocumentIndexingPreparation prepareDocumentAndChunksForIndexing(UUID documentId, String traceId) {
        return transactionTemplate.execute(status -> {
            DocumentEntity document = findDocumentWithLock(documentId);
            List<DocumentChunkEntity> persistedChunks =
                    documentChunkRepository.findByDocumentIdOrderByChunkIndexAsc(documentId);
            if (document.getStatus() == DocumentStatus.INDEXED
                    && !persistedChunks.isEmpty()
                    && persistedChunks.stream().allMatch(chunk -> chunk.getStatus() == DocumentChunkStatus.INDEXED)) {
                return DocumentIndexingPreparation.alreadyIndexed(document, persistedChunks.size());
            }
            if (document.getStatus() != DocumentStatus.EMBEDDING) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "only embedding documents can be indexed");
            }
            List<DocumentChunkEntity> chunks = documentChunkRepository
                    .findByDocumentIdAndStatusOrderByChunkIndexAsc(documentId, DocumentChunkStatus.PENDING_EMBEDDING);
            if (chunks.isEmpty()) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "document has no pending chunks to index");
            }
            document.markEmbedding(traceId);
            chunks.forEach(chunk -> chunk.markEmbedding(traceId));
            documentRepository.saveAndFlush(document);
            return DocumentIndexingPreparation.pending(document, documentChunkRepository.saveAllAndFlush(chunks));
        });
    }

    private void markFailed(
            UUID documentId,
            List<DocumentChunkEntity> chunks,
            String summary,
            String traceId) {
        transactionTemplate.executeWithoutResult(status -> {
            documentRepository.findById(documentId).ifPresent(document -> {
                document.markFailed(summary, traceId);
                documentRepository.saveAndFlush(document);
            });
            if (chunks != null && !chunks.isEmpty()) {
                List<UUID> chunkIds = chunks.stream()
                        .map(DocumentChunkEntity::getId)
                        .toList();
                List<DocumentChunkEntity> persistedChunks = documentChunkRepository.findAllById(chunkIds);
                persistedChunks.forEach(chunk -> chunk.markFailed(summary, traceId));
                documentChunkRepository.saveAllAndFlush(persistedChunks);
            }
        });
    }

    private DocumentEntity findDocument(UUID documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("document not found"));
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

    private record DocumentIndexingPreparation(
            DocumentEntity document,
            List<DocumentChunkEntity> chunks,
            int chunkCount,
            boolean alreadyIndexed) {

        private static DocumentIndexingPreparation pending(
                DocumentEntity document,
                List<DocumentChunkEntity> chunks) {
            return new DocumentIndexingPreparation(document, chunks, chunks.size(), false);
        }

        private static DocumentIndexingPreparation alreadyIndexed(DocumentEntity document, int chunkCount) {
            return new DocumentIndexingPreparation(document, List.of(), chunkCount, true);
        }
    }
}
