package com.agentweave.knowledge.messaging.publisher;

import com.agentweave.knowledge.domain.DocumentEntity;
import com.agentweave.knowledge.messaging.event.DocumentProcessingEvent;
import com.agentweave.knowledge.messaging.event.DocumentProcessingEventType;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "agentweave.document-pipeline.rabbitmq", name = "enabled", havingValue = "true")
public class DocumentReindexRequestedEventPublisher {

    private final DocumentProcessingPublisher documentProcessingPublisher;

    public DocumentReindexRequestedEventPublisher(DocumentProcessingPublisher documentProcessingPublisher) {
        this.documentProcessingPublisher = documentProcessingPublisher;
    }

    public void publish(DocumentEntity document, String traceId, UUID triggeredBy) {
        documentProcessingPublisher.publish(DocumentProcessingEvent.create(
                DocumentProcessingEventType.DOCUMENT_REINDEX_REQUESTED,
                document.getId(),
                traceId,
                triggeredBy,
                metadata(document)));
    }

    private Map<String, String> metadata(DocumentEntity document) {
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("source", document.getSource());
        metadata.put("businessDomain", document.getBusinessDomain());
        metadata.put("documentType", document.getDocumentType());
        metadata.put("permissionLevel", document.getPermissionLevel());
        metadata.put("operation", "reindex");
        metadata.put("reindexCount", Integer.toString(document.getReindexCount()));
        return metadata;
    }
}
