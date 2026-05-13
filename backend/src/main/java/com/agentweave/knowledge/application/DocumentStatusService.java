package com.agentweave.knowledge.application;

import com.agentweave.knowledge.domain.DocumentEntity;
import com.agentweave.knowledge.repository.DocumentRepository;
import com.agentweave.shared.exception.ResourceNotFoundException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class DocumentStatusService {

    private final DocumentRepository documentRepository;
    private final TransactionTemplate transactionTemplate;

    public DocumentStatusService(
            DocumentRepository documentRepository,
            TransactionTemplate transactionTemplate) {
        this.documentRepository = documentRepository;
        this.transactionTemplate = transactionTemplate;
    }

    public void markUploaded(DocumentEntity document, String traceId) {
        document.markUploaded(traceId);
    }

    public void markParsing(DocumentEntity document, String traceId) {
        document.markParsing(traceId);
    }

    public void markCleaning(DocumentEntity document, String cleanedText, int textLength, String traceId) {
        document.markCleaning(cleanedText, textLength, traceId);
    }

    public void markEmbedding(DocumentEntity document, String traceId) {
        document.markEmbedding(traceId);
    }

    public void markChunking(UUID documentId, String traceId) {
        transactionTemplate.executeWithoutResult(status -> {
            DocumentEntity document = findDocument(documentId);
            document.markChunking(traceId);
            documentRepository.saveAndFlush(document);
        });
    }

    public void markIndexed(UUID documentId, String traceId) {
        transactionTemplate.executeWithoutResult(status -> {
            DocumentEntity document = findDocument(documentId);
            document.markIndexed(traceId);
            documentRepository.saveAndFlush(document);
        });
    }

    public void markFailed(UUID documentId, String errorMessage, String traceId) {
        transactionTemplate.executeWithoutResult(status -> documentRepository.findById(documentId)
                .ifPresent(document -> {
                    document.markFailed(errorMessage, traceId);
                    documentRepository.saveAndFlush(document);
                }));
    }

    private DocumentEntity findDocument(UUID documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("document not found"));
    }
}
