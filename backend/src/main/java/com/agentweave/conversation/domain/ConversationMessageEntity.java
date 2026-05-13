package com.agentweave.conversation.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "conversation_messages")
public class ConversationMessageEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private ConversationEntity conversation;

    @Column(nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private MessageRole role;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private MessageStatus status;

    @Column(length = 120)
    private String errorCode;

    @Column(length = 500)
    private String errorMessage;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String metadata = "{}";

    @Column(length = 120)
    private String traceId;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    protected ConversationMessageEntity() {
    }

    public ConversationMessageEntity(
            UUID id,
            UUID userId,
            MessageRole role,
            String content,
            MessageStatus status,
            String traceId) {
        this.id = id;
        this.userId = userId;
        this.role = role;
        this.content = content;
        this.status = status;
        this.traceId = traceId;
    }

    void attachTo(ConversationEntity conversation) {
        this.conversation = conversation;
    }

    public void complete(String content) {
        this.content = content;
        this.status = MessageStatus.SUCCEEDED;
        this.errorCode = null;
        this.errorMessage = null;
    }

    public void stream() {
        this.status = MessageStatus.STREAMING;
    }

    public void fail(String errorCode, String message) {
        this.content = message;
        this.errorCode = errorCode;
        this.errorMessage = message;
        this.status = MessageStatus.FAILED;
    }

    public void cancel(String errorCode, String message) {
        this.errorCode = errorCode;
        this.errorMessage = message;
        this.status = MessageStatus.CANCELLED;
    }

    public UUID getId() {
        return id;
    }

    public ConversationEntity getConversation() {
        return conversation;
    }

    public UUID getUserId() {
        return userId;
    }

    public MessageRole getRole() {
        return role;
    }

    public String getContent() {
        return content;
    }

    public MessageStatus getStatus() {
        return status;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getMetadata() {
        return metadata;
    }

    public String getTraceId() {
        return traceId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
