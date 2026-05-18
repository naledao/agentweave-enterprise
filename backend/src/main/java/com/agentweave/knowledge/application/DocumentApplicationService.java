package com.agentweave.knowledge.application;

import com.agentweave.conversation.repository.ConversationMessageRepository;
import com.agentweave.knowledge.domain.DocumentEntity;
import com.agentweave.knowledge.domain.DocumentChunkEntity;
import com.agentweave.knowledge.domain.DocumentStatus;
import com.agentweave.knowledge.dto.CreateDocumentRequest;
import com.agentweave.knowledge.dto.DocumentCitationRecordResponse;
import com.agentweave.knowledge.dto.DocumentDetailResponse;
import com.agentweave.knowledge.dto.DocumentListResponse;
import com.agentweave.knowledge.dto.DocumentQueryRequest;
import com.agentweave.knowledge.dto.DocumentResponse;
import com.agentweave.knowledge.messaging.publisher.DocumentUploadedEventPublisher;
import com.agentweave.knowledge.repository.DocumentChunkRepository;
import com.agentweave.knowledge.repository.DocumentRepository;
import com.agentweave.graphrag.application.GraphRagIndexLogService;
import com.agentweave.shared.audit.AuditEventType;
import com.agentweave.shared.audit.AuditLog;
import com.agentweave.shared.exception.BusinessException;
import com.agentweave.shared.exception.ErrorCode;
import com.agentweave.shared.exception.ResourceNotFoundException;
import com.agentweave.shared.security.CurrentUser;
import com.agentweave.shared.security.CurrentUserService;
import com.agentweave.shared.tracing.TraceIdProvider;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DocumentApplicationService {

    private static final Logger log = LoggerFactory.getLogger(DocumentApplicationService.class);
    private static final String UPLOAD_PERMISSION = "knowledge:document:upload";
    private static final String PARSE_PERMISSION = "knowledge:document:parse";
    private static final String DELETE_PERMISSION = "knowledge:document:delete";
    private static final int CHECKSUM_BUFFER_SIZE = 8192;
    private static final int DOCUMENT_CITATION_RECORD_LIMIT = 20;

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final ConversationMessageRepository conversationMessageRepository;
    private final DocumentMetadataFactory documentMetadataFactory;
    private final DocumentChunkingService documentChunkingService;
    private final DocumentCleaningService documentCleaningService;
    private final DocumentParserService documentParserService;
    private final DocumentStatusService documentStatusService;
    private final DocumentStorageService documentStorageService;
    private final GraphRagIndexCleanupService graphRagIndexCleanupService;
    private final PgVectorIndexService pgVectorIndexService;
    private final GraphRagIndexLogService graphRagIndexLogService;
    private final KnowledgeStorageProperties storageProperties;
    private final CurrentUserService currentUserService;
    private final TraceIdProvider traceIdProvider;
    private final TransactionTemplate transactionTemplate;
    private final ObjectProvider<DocumentUploadedEventPublisher> documentUploadedEventPublisherProvider;

    public DocumentApplicationService(
            DocumentRepository documentRepository,
            DocumentChunkRepository documentChunkRepository,
            ConversationMessageRepository conversationMessageRepository,
            DocumentMetadataFactory documentMetadataFactory,
            DocumentChunkingService documentChunkingService,
            DocumentCleaningService documentCleaningService,
            DocumentParserService documentParserService,
            DocumentStatusService documentStatusService,
            DocumentStorageService documentStorageService,
            GraphRagIndexCleanupService graphRagIndexCleanupService,
            PgVectorIndexService pgVectorIndexService,
            GraphRagIndexLogService graphRagIndexLogService,
            KnowledgeStorageProperties storageProperties,
            CurrentUserService currentUserService,
            TraceIdProvider traceIdProvider,
            TransactionTemplate transactionTemplate,
            ObjectProvider<DocumentUploadedEventPublisher> documentUploadedEventPublisherProvider) {
        this.documentRepository = documentRepository;
        this.documentChunkRepository = documentChunkRepository;
        this.conversationMessageRepository = conversationMessageRepository;
        this.documentMetadataFactory = documentMetadataFactory;
        this.documentChunkingService = documentChunkingService;
        this.documentCleaningService = documentCleaningService;
        this.documentParserService = documentParserService;
        this.documentStatusService = documentStatusService;
        this.documentStorageService = documentStorageService;
        this.graphRagIndexCleanupService = graphRagIndexCleanupService;
        this.pgVectorIndexService = pgVectorIndexService;
        this.graphRagIndexLogService = graphRagIndexLogService;
        this.storageProperties = storageProperties;
        this.currentUserService = currentUserService;
        this.traceIdProvider = traceIdProvider;
        this.transactionTemplate = transactionTemplate;
        this.documentUploadedEventPublisherProvider = documentUploadedEventPublisherProvider;
    }

    @Transactional
    @AuditLog(
            eventType = AuditEventType.DOCUMENT_UPLOAD,
            resourceType = "document",
            resourceId = "#result.documentId",
            includeResponse = false)
    public DocumentResponse upload(MultipartFile file, CreateDocumentRequest request) {
        CurrentUser user = currentUserService.requireCurrentUser();
        currentUserService.requirePermission(UPLOAD_PERMISSION);
        validateMetadata(request);
        validateFile(file);

        UUID documentId = UUID.randomUUID();
        String originalFilename = safeFilename(file.getOriginalFilename());
        String contentType = normalizeContentType(file.getContentType());
        String objectKey = buildObjectKey(user.id(), documentId, originalFilename);
        String checksum = checksum(file);
        StoredDocumentObject storedObject = storeObject(file, objectKey, contentType, checksum);
        String traceId = traceIdProvider.currentTraceId();

        try {
            DocumentEntity document = new DocumentEntity(
                    documentId,
                    originalFilename,
                    contentType,
                    file.getSize(),
                    storedObject.bucket(),
                    storedObject.objectKey(),
                    storedObject.checksum(),
                    user.id(),
                    request.source().trim(),
                    request.businessDomain().trim(),
                    request.documentType().trim(),
                    request.permissionLevel().trim(),
                    request.effectiveFrom(),
                    request.effectiveTo(),
                    String.join(",", request.normalizedTags()));
            documentStatusService.markUploaded(document, traceId);
            DocumentEntity savedDocument = documentRepository.saveAndFlush(document);
            publishUploadedAfterCommit(savedDocument, traceId);
            return DocumentResponse.from(savedDocument, 0);
        } catch (DataAccessException ex) {
            documentStorageService.delete(storedObject.bucket(), storedObject.objectKey());
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "failed to create document record");
        }
    }

    @Transactional(readOnly = true)
    public DocumentListResponse list(DocumentQueryRequest request) {
        currentUserService.requireCurrentUser();
        Pageable pageable = PageRequest.of(
                request.pageNumber(),
                request.pageSize(),
                Sort.by(Sort.Direction.DESC, "updatedAt"));
        String keyword = request.normalizedKeyword();
        Page<DocumentEntity> documents = keyword == null
                ? documentRepository.findAll(pageable)
                : documentRepository.findByFilenameContainingIgnoreCase(keyword, pageable);
        Page<DocumentResponse> responses = documents
                .map(document -> DocumentResponse.from(document, documentChunkRepository.countByDocumentId(document.getId())));
        return DocumentListResponse.from(responses);
    }

    @Transactional(readOnly = true)
    public DocumentDetailResponse get(UUID documentId) {
        CurrentUser user = currentUserService.requireCurrentUser();
        DocumentEntity document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("document not found"));
        List<DocumentChunkEntity> chunks = documentChunkRepository.findByDocumentIdOrderByChunkIndexAsc(documentId);
        List<DocumentCitationRecordResponse> citationRecords = conversationMessageRepository
                .findRecentAssistantMessagesReferencingDocument(
                        user.id(),
                        documentId.toString(),
                        DOCUMENT_CITATION_RECORD_LIMIT)
                .stream()
                .map(DocumentCitationRecordResponse::from)
                .toList();
        return DocumentDetailResponse.from(
                document,
                chunks,
                graphRagIndexLogService.latestSummary(documentId),
                citationRecords);
    }

    @Transactional
    public void delete(UUID documentId) {
        currentUserService.requireCurrentUser();
        currentUserService.requirePermission(DELETE_PERMISSION);
        String traceId = traceIdProvider.currentTraceId();

        DocumentEntity document = documentRepository.findWithLockById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("document not found"));
        if (document.getStatus() == DocumentStatus.PARSING
                || document.getStatus() == DocumentStatus.CLEANING
                || document.getStatus() == DocumentStatus.CHUNKING
                || document.getStatus() == DocumentStatus.EMBEDDING) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "document is being processed");
        }

        List<UUID> chunkIds = documentChunkRepository.findByDocumentIdOrderByChunkIndexAsc(documentId).stream()
                .map(DocumentChunkEntity::getId)
                .toList();
        try {
            graphRagIndexCleanupService.deleteByDocumentId(documentId, chunkIds, traceId);
            pgVectorIndexService.deleteByDocumentId(documentId);
            transactionTemplate.executeWithoutResult(status -> {
                documentChunkRepository.deleteByDocumentId(documentId);
                documentRepository.delete(document);
            });
            documentStorageService.delete(document.getStorageBucket(), document.getStorageObjectKey());
            log.info("Document deleted: documentId={}, traceId={}, chunkCount={}",
                    documentId,
                    traceId,
                    chunkIds.size());
        } catch (RuntimeException ex) {
            String summary = errorSummary(ex);
            log.warn("Document delete failed: documentId={}, traceId={}, error={}", documentId, traceId, summary, ex);
            throw ex instanceof BusinessException businessException
                    ? businessException
                    : new BusinessException(ErrorCode.BUSINESS_ERROR, summary);
        }
    }

    public DocumentResponse parseDocument(UUID documentId) {
        currentUserService.requireCurrentUser();
        currentUserService.requirePermission(PARSE_PERMISSION);
        String traceId = traceIdProvider.currentTraceId();
        DocumentEntity cleanedDocument = parseUploadedDocument(documentId, traceId);
        return DocumentResponse.from(
                cleanedDocument,
                documentChunkRepository.countByDocumentId(cleanedDocument.getId()));
    }

    public DocumentEntity parseUploadedDocument(UUID documentId, String traceId) {
        DocumentEntity parsingDocument = transactionTemplate.execute(status -> {
            DocumentEntity document = documentRepository.findWithLockById(documentId)
                    .orElseThrow(() -> new ResourceNotFoundException("document not found"));
            if (document.getStatus() == DocumentStatus.CLEANING
                    && document.getCleanedText() != null
                    && !document.getCleanedText().isBlank()) {
                return document;
            }
            if (document.getStatus() != DocumentStatus.UPLOADED) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "only uploaded documents can be parsed");
            }
            documentStatusService.markParsing(document, traceId);
            return documentRepository.saveAndFlush(document);
        });

        if (parsingDocument.getStatus() == DocumentStatus.CLEANING) {
            log.info("Document already cleaned, skipping parse: documentId={}, traceId={}", documentId, traceId);
            return parsingDocument;
        }

        try (InputStream inputStream = documentStorageService.read(
                parsingDocument.getStorageBucket(),
                parsingDocument.getStorageObjectKey())) {
            DocumentParseResult parseResult = documentParserService.parse(
                    parsingDocument.getFilename(),
                    parsingDocument.getContentType(),
                    inputStream);
            DocumentCleanResult cleanResult = documentCleaningService.clean(parseResult.text());
            DocumentEntity cleanedDocument = transactionTemplate.execute(status -> {
                DocumentEntity document = documentRepository.findById(documentId)
                        .orElseThrow(() -> new ResourceNotFoundException("document not found"));
                documentStatusService.markCleaning(
                        document,
                        cleanResult.cleanedText(),
                        cleanResult.textLength(),
                        traceId);
                return documentRepository.saveAndFlush(document);
            });
            log.info(
                    "Document cleaned: documentId={}, traceId={}, originalLength={}, cleanedLength={}",
                    documentId,
                    traceId,
                    cleanResult.originalLength(),
                    cleanResult.textLength());
            return cleanedDocument;
        } catch (RuntimeException | IOException ex) {
            String summary = errorSummary(ex);
            log.warn("Document parsing failed: documentId={}, traceId={}, error={}", documentId, traceId, summary, ex);
            documentStatusService.markFailed(documentId, summary, traceId);
            if (ex instanceof BusinessException businessException) {
                throw businessException;
            }
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, summary);
        }
    }

    public List<DocumentChunkEntity> chunkDocument(UUID documentId, String cleanedText) {
        String traceId = traceIdProvider.currentTraceId();
        documentStatusService.markChunking(documentId, traceId);

        try {
            List<String> chunks = documentChunkingService.split(cleanedText);
            return transactionTemplate.execute(status -> {
                DocumentEntity document = documentRepository.findById(documentId)
                        .orElseThrow(() -> new ResourceNotFoundException("document not found"));
                List<DocumentChunkEntity> savedChunks = replaceChunks(document, chunks);
                documentStatusService.markEmbedding(document, traceId);
                documentRepository.saveAndFlush(document);
                return savedChunks;
            });
        } catch (RuntimeException ex) {
            documentStatusService.markFailed(documentId, errorSummary(ex), traceId);
            throw ex;
        }
    }

    @Transactional
    public List<DocumentChunkEntity> replaceChunks(UUID documentId, List<String> chunkContents) {
        DocumentEntity document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("document not found"));
        return replaceChunks(document, chunkContents);
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

    private String errorSummary(Exception ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            message = ex.getClass().getSimpleName();
        }
        return message.length() > 1000 ? message.substring(0, 1000) : message;
    }

    private void publishUploadedAfterCommit(DocumentEntity document, String traceId) {
        DocumentUploadedEventPublisher publisher = documentUploadedEventPublisherProvider.getIfAvailable();
        if (publisher == null) {
            log.debug("Document uploaded event publisher is disabled: documentId={}, traceId={}", document.getId(), traceId);
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            publishUploaded(publisher, document, traceId);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                publishUploaded(publisher, document, traceId);
            }
        });
    }

    private void publishUploaded(DocumentUploadedEventPublisher publisher, DocumentEntity document, String traceId) {
        try {
            publisher.publish(document, traceId);
        } catch (RuntimeException ex) {
            log.warn(
                    "Document uploaded event publish failed after commit: documentId={}, traceId={}, error={}",
                    document.getId(),
                    traceId,
                    errorSummary(ex),
                    ex);
        }
    }

    private void validateMetadata(CreateDocumentRequest request) {
        if (request.effectiveFrom() != null
                && request.effectiveTo() != null
                && request.effectiveFrom().isAfter(request.effectiveTo())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "effectiveFrom must be before effectiveTo");
        }
        if (request.normalizedTags().size() > 20) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "tags must not exceed 20 items");
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "file must not be empty");
        }
        if (file.getSize() > storageProperties.maxFileSize().toBytes()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "file size exceeds 50MB limit");
        }
        String contentType = normalizeContentType(file.getContentType());
        if (!storageProperties.allowedContentTypes().contains(contentType)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "unsupported file content type");
        }
        String extension = extension(safeFilename(file.getOriginalFilename()));
        if (!storageProperties.allowedExtensions().contains(extension)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "unsupported file extension");
        }
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

    private String checksum(MultipartFile file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream inputStream = file.getInputStream();
                    DigestInputStream digestInputStream = new DigestInputStream(inputStream, digest)) {
                byte[] buffer = new byte[CHECKSUM_BUFFER_SIZE];
                while (digestInputStream.read(buffer) != -1) {
                    // Drain the stream so DigestInputStream can update the checksum.
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (IOException | NoSuchAlgorithmException ex) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "failed to calculate document checksum");
        }
    }

    private StoredDocumentObject storeObject(
            MultipartFile file,
            String objectKey,
            String contentType,
            String checksum) {
        try (InputStream inputStream = file.getInputStream()) {
            return documentStorageService.store(objectKey, inputStream, file.getSize(), contentType, checksum);
        } catch (IOException | DocumentStorageException ex) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "failed to store document object");
        }
    }

    private String buildObjectKey(UUID userId, UUID documentId, String filename) {
        String date = Instant.now().toString().substring(0, 10);
        return "documents/%s/%s/%s/%s".formatted(date, userId, documentId, filename);
    }

    private String safeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "document";
        }
        String normalized = Normalizer.normalize(filename.trim(), Normalizer.Form.NFKC)
                .replace("\\", "_")
                .replace("/", "_");
        if (normalized.length() > 180) {
            String suffix = normalized.substring(Math.max(0, normalized.lastIndexOf('.')));
            String base = normalized.substring(0, Math.min(140, normalized.length()));
            return base + suffix;
        }
        return normalized;
    }

    private String normalizeContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return "application/octet-stream";
        }
        return contentType.trim().toLowerCase(Locale.ROOT);
    }

    private String extension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }
}
