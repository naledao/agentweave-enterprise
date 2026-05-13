package com.agentweave.knowledge.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agentweave.knowledge.domain.DocumentChunkEntity;
import com.agentweave.knowledge.domain.DocumentStatus;
import com.agentweave.knowledge.domain.DocumentEntity;
import com.agentweave.knowledge.repository.DocumentChunkRepository;
import com.agentweave.knowledge.repository.DocumentRepository;
import com.agentweave.shared.exception.BusinessException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

class DocumentReindexPipelineServiceTest {

    private final DocumentRepository documentRepository =
            org.mockito.Mockito.mock(DocumentRepository.class);
    private final DocumentChunkRepository documentChunkRepository =
            org.mockito.Mockito.mock(DocumentChunkRepository.class);
    private final DocumentMetadataFactory documentMetadataFactory = new DocumentMetadataFactory();
    private final DocumentChunkingService documentChunkingService =
            org.mockito.Mockito.mock(DocumentChunkingService.class);
    private final GraphRagIndexCleanupService graphRagIndexCleanupService =
            org.mockito.Mockito.mock(GraphRagIndexCleanupService.class);
    private final PgVectorIndexService pgVectorIndexService =
            org.mockito.Mockito.mock(PgVectorIndexService.class);
    private final TransactionTemplate transactionTemplate = new TransactionTemplate(new ImmediateTransactionManager());
    private final DocumentReindexPipelineService service = new DocumentReindexPipelineService(
            documentRepository,
            documentChunkRepository,
            documentMetadataFactory,
            documentChunkingService,
            graphRagIndexCleanupService,
            pgVectorIndexService,
            transactionTemplate);

    private UUID documentId;
    private DocumentEntity document;
    private DocumentChunkEntity oldChunk;

    @BeforeEach
    void setUp() {
        documentId = UUID.randomUUID();
        document = document(documentId);
        document.markCleaning("first searchable chunk\n\nsecond searchable chunk", 46, "trace-clean");
        document.markIndexed("trace-index");
        oldChunk = new DocumentChunkEntity(
                UUID.randomUUID(),
                documentId,
                0,
                "old searchable chunk",
                java.util.Map.of("documentId", documentId.toString()));

        when(documentRepository.findWithLockById(documentId)).thenReturn(Optional.of(document));
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
        when(documentRepository.saveAndFlush(any(DocumentEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0, DocumentEntity.class));
    }

    @Test
    void cleansOldIndexesReplacesChunksAndMarksEmbedding() {
        when(documentChunkRepository.findByDocumentIdOrderByChunkIndexAsc(documentId))
                .thenReturn(List.of(oldChunk));
        when(documentChunkingService.split(document.getCleanedText()))
                .thenReturn(List.of("first searchable chunk", "second searchable chunk"));
        when(documentChunkRepository.saveAllAndFlush(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DocumentChunkingResult result = service.prepareReindex(documentId, "trace-reindex");

        Assertions.assertThat(result.document()).isSameAs(document);
        Assertions.assertThat(result.chunkCount()).isEqualTo(2);
        Assertions.assertThat(document.getStatus()).isEqualTo(DocumentStatus.EMBEDDING);
        Assertions.assertThat(document.getTraceId()).isEqualTo("trace-reindex");
        verify(graphRagIndexCleanupService)
                .deleteByDocumentId(documentId, List.of(oldChunk.getId()), "trace-reindex");
        verify(pgVectorIndexService).deleteByDocumentId(documentId);
        verify(documentChunkRepository).deleteByDocumentId(documentId);
    }

    @Test
    void rejectsDocumentAlreadyBeingProcessed() {
        document.markEmbedding("trace-existing");

        Assertions.assertThatThrownBy(() -> service.prepareReindex(documentId, "trace-reindex"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("document is already being processed");

        verify(graphRagIndexCleanupService, never()).deleteByDocumentId(any(), any(), any());
        verify(pgVectorIndexService, never()).deleteByDocumentId(any());
    }

    @Test
    void rejectsDocumentWithoutCleanedText() {
        DocumentEntity unparsedDocument = document(UUID.randomUUID());
        when(documentRepository.findWithLockById(unparsedDocument.getId()))
                .thenReturn(Optional.of(unparsedDocument));

        Assertions.assertThatThrownBy(() -> service.prepareReindex(unparsedDocument.getId(), "trace-reindex"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("document must be parsed before reindexing");

        verify(graphRagIndexCleanupService, never()).deleteByDocumentId(any(), any(), any());
        verify(pgVectorIndexService, never()).deleteByDocumentId(any());
    }

    @Test
    void marksFailedWhenVectorCleanupFails() {
        when(documentChunkRepository.findByDocumentIdOrderByChunkIndexAsc(documentId))
                .thenReturn(List.of(oldChunk));
        doThrow(new IllegalStateException("vector cleanup unavailable"))
                .when(pgVectorIndexService)
                .deleteByDocumentId(documentId);

        Assertions.assertThatThrownBy(() -> service.prepareReindex(documentId, "trace-reindex"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("vector cleanup unavailable");

        Assertions.assertThat(document.getStatus()).isEqualTo(DocumentStatus.FAILED);
        Assertions.assertThat(document.getErrorMessage()).isEqualTo("vector cleanup unavailable");
        Assertions.assertThat(document.getTraceId()).isEqualTo("trace-reindex");
    }

    @Test
    void returnsExistingPreparedChunksForSameTraceRetry() {
        document.markEmbedding("trace-reindex");
        when(documentChunkRepository.countByDocumentId(documentId)).thenReturn(2L);

        DocumentChunkingResult result = service.prepareReindex(documentId, "trace-reindex");

        Assertions.assertThat(result.document()).isSameAs(document);
        Assertions.assertThat(result.chunkCount()).isEqualTo(2);
        verify(graphRagIndexCleanupService, never()).deleteByDocumentId(any(), any(), any());
        verify(pgVectorIndexService, never()).deleteByDocumentId(any());
        verify(documentChunkingService, never()).split(any());
    }

    private DocumentEntity document(UUID id) {
        return new DocumentEntity(
                id,
                "runbook.txt",
                "text/plain",
                12,
                "agentweave-documents",
                "documents/runbook.txt",
                "checksum",
                UUID.randomUUID(),
                "runbook",
                "ops",
                "faq",
                "internal",
                null,
                null,
                "");
    }

    private static final class ImmediateTransactionManager implements PlatformTransactionManager {

        @Override
        public TransactionStatus getTransaction(TransactionDefinition definition) {
            return new SimpleTransactionStatus();
        }

        @Override
        public void commit(TransactionStatus status) {
        }

        @Override
        public void rollback(TransactionStatus status) {
        }
    }
}
