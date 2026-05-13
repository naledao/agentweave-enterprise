package com.agentweave.knowledge.messaging.publisher;

import com.agentweave.knowledge.domain.DocumentEntity;
import com.agentweave.knowledge.messaging.event.DocumentProcessingEvent;
import com.agentweave.knowledge.messaging.event.DocumentProcessingEventType;
import com.agentweave.knowledge.repository.DocumentRepository;
import com.agentweave.shared.exception.ResourceNotFoundException;
import java.util.Map;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "agentweave.document-pipeline.rabbitmq", name = "enabled", havingValue = "true")
public class DocumentVectorIndexedEventPublisher {

    private final DocumentRepository documentRepository;
    private final DocumentProcessingPublisher documentProcessingPublisher;

    public DocumentVectorIndexedEventPublisher(
            DocumentRepository documentRepository,
            DocumentProcessingPublisher documentProcessingPublisher) {
        this.documentRepository = documentRepository;
        this.documentProcessingPublisher = documentProcessingPublisher;
    }

    public void publish(UUID documentId, String traceId) {
        DocumentEntity document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("document not found"));
        publish(document, traceId);
    }

    public void publish(DocumentEntity document, String traceId) {
        documentProcessingPublisher.publish(DocumentProcessingEvent.create(
                DocumentProcessingEventType.DOCUMENT_VECTOR_INDEXED,
                document.getId(),
                traceId,
                document.getUploadedBy(),
                Map.of(
                        "source", document.getSource(),
                        "businessDomain", document.getBusinessDomain(),
                        "documentType", document.getDocumentType(),
                        "permissionLevel", document.getPermissionLevel())));
    }
}
