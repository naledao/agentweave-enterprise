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
public class DocumentChunkedEventPublisher {

    private final DocumentProcessingPublisher documentProcessingPublisher;

    public DocumentChunkedEventPublisher(DocumentProcessingPublisher documentProcessingPublisher) {
        this.documentProcessingPublisher = documentProcessingPublisher;
    }

    public void publish(DocumentEntity document, int chunkCount, String traceId) {
        documentProcessingPublisher.publish(DocumentProcessingEvent.create(
                DocumentProcessingEventType.DOCUMENT_CHUNKED,
                document.getId(),
                traceId,
                document.getUploadedBy(),
                metadata(document, chunkCount)));
    }

    private Map<String, String> metadata(DocumentEntity document, int chunkCount) {
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("source", document.getSource());
        metadata.put("businessDomain", document.getBusinessDomain());
        metadata.put("documentType", document.getDocumentType());
        metadata.put("permissionLevel", document.getPermissionLevel());
        metadata.put("chunkCount", Integer.toString(chunkCount));
        if (document.getTextLength() != null) {
            metadata.put("textLength", document.getTextLength().toString());
        }
        return metadata;
    }
}
