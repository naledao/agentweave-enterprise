package com.agentweave.knowledge.messaging.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "document_message_consumptions")
public class DocumentMessageConsumptionEntity {

    @Id
    private UUID eventId;

    @Column(nullable = false, length = 120)
    private String eventType;

    @Column(nullable = false)
    private UUID documentId;

    @Column(nullable = false, length = 120)
    private String traceId;

    @Column(nullable = false, length = 120)
    private String consumer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private DocumentMessageConsumptionStatus status;

    @Column(nullable = false)
    private Instant processedAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    protected DocumentMessageConsumptionEntity() {
    }

    public DocumentMessageConsumptionEntity(
            UUID eventId,
            String eventType,
            UUID documentId,
            String traceId,
            String consumer) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.documentId = documentId;
        this.traceId = traceId;
        this.consumer = consumer;
        this.status = DocumentMessageConsumptionStatus.PROCESSED;
        this.processedAt = Instant.now();
    }

    public UUID getEventId() {
        return eventId;
    }
}
