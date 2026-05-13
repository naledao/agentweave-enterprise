package com.agentweave.knowledge.messaging.application;

import com.agentweave.knowledge.messaging.domain.DocumentMessageConsumptionEntity;
import com.agentweave.knowledge.messaging.event.DocumentProcessingEvent;
import com.agentweave.knowledge.messaging.repository.DocumentMessageConsumptionRepository;
import org.springframework.stereotype.Service;

@Service
public class DocumentMessageIdempotencyService {

    private final DocumentMessageConsumptionRepository consumptionRepository;

    public DocumentMessageIdempotencyService(DocumentMessageConsumptionRepository consumptionRepository) {
        this.consumptionRepository = consumptionRepository;
    }

    public boolean isProcessed(DocumentProcessingEvent event) {
        return consumptionRepository.existsById(event.eventId());
    }

    public void markProcessed(DocumentProcessingEvent event, String consumer) {
        consumptionRepository.saveAndFlush(new DocumentMessageConsumptionEntity(
                event.eventId(),
                event.eventType(),
                event.documentId(),
                event.traceId(),
                consumer));
    }
}
