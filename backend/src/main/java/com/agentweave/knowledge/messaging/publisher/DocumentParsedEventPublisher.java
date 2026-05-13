package com.agentweave.knowledge.messaging.publisher;

import com.agentweave.knowledge.domain.DocumentEntity;
import com.agentweave.knowledge.messaging.event.DocumentProcessingEvent;
import com.agentweave.knowledge.messaging.event.DocumentProcessingEventType;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "agentweave.document-pipeline.rabbitmq", name = "enabled", havingValue = "true")
public class DocumentParsedEventPublisher {

    private final DocumentProcessingPublisher documentProcessingPublisher;

    public DocumentParsedEventPublisher(DocumentProcessingPublisher documentProcessingPublisher) {
        this.documentProcessingPublisher = documentProcessingPublisher;
    }

    public void publish(DocumentEntity document, String traceId) {
        documentProcessingPublisher.publish(DocumentProcessingEvent.create(
                DocumentProcessingEventType.DOCUMENT_PARSED,
                document.getId(),
                traceId,
                document.getUploadedBy(),
                metadata(document)));
    }

    private Map<String, String> metadata(DocumentEntity document) {
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("source", document.getSource());
        metadata.put("businessDomain", document.getBusinessDomain());
        metadata.put("documentType", document.getDocumentType());
        metadata.put("permissionLevel", document.getPermissionLevel());
        if (document.getTextLength() != null) {
            metadata.put("textLength", document.getTextLength().toString());
        }
        return metadata;
    }
}
