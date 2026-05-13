package com.agentweave.knowledge.application;

import com.agentweave.knowledge.domain.DocumentChunkEntity;
import com.agentweave.knowledge.domain.DocumentEntity;
import com.agentweave.knowledge.domain.DocumentStatus;
import com.agentweave.knowledge.repository.DocumentChunkRepository;
import com.agentweave.knowledge.repository.DocumentRepository;
import com.agentweave.shared.exception.BusinessException;
import com.agentweave.shared.exception.ErrorCode;
import com.agentweave.shared.exception.ResourceNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class DocumentChunkPipelineService {

    private static final Logger log = LoggerFactory.getLogger(DocumentChunkPipelineService.class);

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final DocumentMetadataFactory documentMetadataFactory;
    private final DocumentChunkingService documentChunkingService;
    private final DocumentStatusService documentStatusService;
    private final TransactionTemplate transactionTemplate;

    public DocumentChunkPipelineService(
            DocumentRepository documentRepository,
            DocumentChunkRepository documentChunkRepository,
            DocumentMetadataFactory documentMetadataFactory,
            DocumentChunkingService documentChunkingService,
            DocumentStatusService documentStatusService,
            TransactionTemplate transactionTemplate) {
        this.documentRepository = documentRepository;
        this.documentChunkRepository = documentChunkRepository;
        this.documentMetadataFactory = documentMetadataFactory;
        this.documentChunkingService = documentChunkingService;
        this.documentStatusService = documentStatusService;
        this.transactionTemplate = transactionTemplate;
    }

    public DocumentChunkingResult chunkParsedDocument(UUID documentId, String traceId) {
        DocumentEntity chunkingDocument = prepareChunking(documentId, traceId);
        if (chunkingDocument.getStatus() == DocumentStatus.EMBEDDING) {
            int chunkCount = Math.toIntExact(documentChunkRepository.countByDocumentId(documentId));
            log.info("Document already chunked, skipping chunk generation: documentId={}, traceId={}",
                    documentId,
                    traceId);
            return new DocumentChunkingResult(chunkingDocument, chunkCount);
        }

        try {
            List<String> chunkContents = documentChunkingService.split(chunkingDocument.getCleanedText());
            DocumentChunkingResult result = transactionTemplate.execute(status -> {
                DocumentEntity document = documentRepository.findWithLockById(documentId)
                        .orElseThrow(() -> new ResourceNotFoundException("document not found"));
                List<DocumentChunkEntity> chunks = replaceChunks(document, chunkContents);
                documentStatusService.markEmbedding(document, traceId);
                DocumentEntity savedDocument = documentRepository.saveAndFlush(document);
                return new DocumentChunkingResult(savedDocument, chunks.size());
            });
            log.info("Document chunked: documentId={}, traceId={}, chunkCount={}",
                    documentId,
                    traceId,
                    result.chunkCount());
            return result;
        } catch (RuntimeException ex) {
            String summary = errorSummary(ex);
            log.warn("Document chunking failed: documentId={}, traceId={}, error={}", documentId, traceId, summary, ex);
            documentStatusService.markFailed(documentId, summary, traceId);
            if (ex instanceof BusinessException businessException) {
                throw businessException;
            }
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, summary);
        }
    }

    private DocumentEntity prepareChunking(UUID documentId, String traceId) {
        return transactionTemplate.execute(status -> {
            DocumentEntity document = documentRepository.findWithLockById(documentId)
                    .orElseThrow(() -> new ResourceNotFoundException("document not found"));
            if (document.getStatus() == DocumentStatus.EMBEDDING
                    && documentChunkRepository.countByDocumentId(documentId) > 0) {
                return document;
            }
            if (document.getStatus() != DocumentStatus.CLEANING) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "only cleaned documents can be chunked");
            }
            document.markChunking(traceId);
            return documentRepository.saveAndFlush(document);
        });
    }

    private List<DocumentChunkEntity> replaceChunks(DocumentEntity document, List<String> chunkContents) {
        List<String> normalizedChunks = normalizeChunkContents(chunkContents);
        documentChunkRepository.deleteByDocumentId(document.getId());

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
        return documentChunkRepository.saveAllAndFlush(chunks);
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

    private String errorSummary(Exception ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            message = ex.getClass().getSimpleName();
        }
        return message.length() > 1000 ? message.substring(0, 1000) : message;
    }
}
