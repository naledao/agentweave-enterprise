package com.agentweave.conversation.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "model_call_logs")
public class ModelCallLogEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID conversationId;

    @Column(nullable = false)
    private UUID messageId;

    @Column(nullable = false, length = 80)
    private String provider;

    @Column(nullable = false, length = 160)
    private String model;

    private Integer promptTokens;

    private Integer completionTokens;

    @Column(nullable = false)
    private long latencyMs;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ModelCallStatus status;

    @Column(length = 120)
    private String errorCode;

    @Column(length = 500)
    private String errorMessage;

    @Column(nullable = false, length = 120)
    private String traceId;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected ModelCallLogEntity() {
    }

    public ModelCallLogEntity(
            UUID id,
            UUID conversationId,
            UUID messageId,
            String provider,
            String model,
            Integer promptTokens,
            Integer completionTokens,
            long latencyMs,
            ModelCallStatus status,
            String errorCode,
            String errorMessage,
            String traceId) {
        this.id = id;
        this.conversationId = conversationId;
        this.messageId = messageId;
        this.provider = provider;
        this.model = model;
        this.promptTokens = promptTokens;
        this.completionTokens = completionTokens;
        this.latencyMs = latencyMs;
        this.status = status;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.traceId = traceId;
    }

    public UUID getId() {
        return id;
    }

    public UUID getConversationId() {
        return conversationId;
    }

    public UUID getMessageId() {
        return messageId;
    }

    public String getProvider() {
        return provider;
    }

    public String getModel() {
        return model;
    }

    public Integer getPromptTokens() {
        return promptTokens;
    }

    public Integer getCompletionTokens() {
        return completionTokens;
    }

    public long getLatencyMs() {
        return latencyMs;
    }

    public ModelCallStatus getStatus() {
        return status;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getTraceId() {
        return traceId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
