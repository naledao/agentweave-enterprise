package com.agentweave.knowledge.messaging.publisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.agentweave.knowledge.domain.DocumentEntity;
import com.agentweave.knowledge.messaging.event.DocumentProcessingEvent;
import com.agentweave.knowledge.messaging.event.DocumentProcessingEventType;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class DocumentReindexRequestedEventPublisherTest {

    private final DocumentProcessingPublisher documentProcessingPublisher =
            org.mockito.Mockito.mock(DocumentProcessingPublisher.class);
    private final DocumentReindexRequestedEventPublisher publisher =
            new DocumentReindexRequestedEventPublisher(documentProcessingPublisher);

    @Test
    void publishesReindexRequestedEventWithTriggeringUserAndMetadata() {
        UUID documentId = UUID.randomUUID();
        UUID uploadedBy = UUID.randomUUID();
        UUID triggeredBy = UUID.randomUUID();
        DocumentEntity document = new DocumentEntity(
                documentId,
                "runbook.txt",
                "text/plain",
                12,
                "agentweave-documents",
                "documents/runbook.txt",
                "checksum",
                uploadedBy,
                "runbook",
                "ops",
                "faq",
                "internal",
                null,
                null,
                "");

        publisher.publish(document, "trace-reindex", triggeredBy);

        ArgumentCaptor<DocumentProcessingEvent> captor = ArgumentCaptor.forClass(DocumentProcessingEvent.class);
        verify(documentProcessingPublisher).publish(captor.capture());
        DocumentProcessingEvent event = captor.getValue();
        assertThat(event.eventType()).isEqualTo(DocumentProcessingEventType.DOCUMENT_REINDEX_REQUESTED.routingKey());
        assertThat(event.documentId()).isEqualTo(documentId);
        assertThat(event.traceId()).isEqualTo("trace-reindex");
        assertThat(event.triggeredBy()).isEqualTo(triggeredBy);
        assertThat(event.metadata())
                .containsEntry("operation", "reindex")
                .containsEntry("source", "runbook")
                .containsEntry("businessDomain", "ops")
                .containsEntry("permissionLevel", "internal");
    }
}
