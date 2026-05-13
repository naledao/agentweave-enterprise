package com.agentweave.knowledge.messaging.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agentweave.knowledge.messaging.domain.DocumentMessageConsumptionEntity;
import com.agentweave.knowledge.messaging.event.DocumentProcessingEvent;
import com.agentweave.knowledge.messaging.event.DocumentProcessingEventType;
import com.agentweave.knowledge.messaging.repository.DocumentMessageConsumptionRepository;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class DocumentMessageIdempotencyServiceTest {

    private final DocumentMessageConsumptionRepository consumptionRepository =
            org.mockito.Mockito.mock(DocumentMessageConsumptionRepository.class);
    private final DocumentMessageIdempotencyService idempotencyService =
            new DocumentMessageIdempotencyService(consumptionRepository);

    @Test
    void recordsProcessedEventId() {
        DocumentProcessingEvent event = DocumentProcessingEvent.create(
                DocumentProcessingEventType.DOCUMENT_UPLOADED,
                UUID.randomUUID(),
                "trace-idempotency",
                UUID.randomUUID(),
                Map.of("source", "runbook"));

        when(consumptionRepository.existsById(event.eventId())).thenReturn(false, true);

        assertThat(idempotencyService.isProcessed(event)).isFalse();

        idempotencyService.markProcessed(event, "document-parse-consumer");

        assertThat(idempotencyService.isProcessed(event)).isTrue();
        ArgumentCaptor<DocumentMessageConsumptionEntity> captor =
                ArgumentCaptor.forClass(DocumentMessageConsumptionEntity.class);
        verify(consumptionRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getEventId()).isEqualTo(event.eventId());
    }
}
