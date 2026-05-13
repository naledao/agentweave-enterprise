package com.agentweave.conversation.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "conversations")
public class ConversationEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID ownerUserId;

    @Column(nullable = false, length = 160)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ConversationStatus status = ConversationStatus.ACTIVE;

    @Column(nullable = false)
    private int messageCount;

    @Column(length = 200)
    private String lastMessagePreview;

    private Instant lastMessageAt;

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<ConversationMessageEntity> messages = new ArrayList<>();

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    protected ConversationEntity() {
    }

    public ConversationEntity(UUID id, UUID ownerUserId, String title) {
        this.id = id;
        this.ownerUserId = ownerUserId;
        this.title = title;
        this.status = ConversationStatus.ACTIVE;
    }

    public void addMessage(ConversationMessageEntity message) {
        this.messages.add(message);
        message.attachTo(this);
        this.messageCount = this.messages.size();
        this.lastMessagePreview = preview(message.getContent());
        this.lastMessageAt = Instant.now();
    }

    public void refreshSummaryFrom(ConversationMessageEntity message) {
        this.lastMessagePreview = preview(message.getContent());
        this.lastMessageAt = Instant.now();
    }

    public void rename(String title) {
        this.title = title;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOwnerUserId() {
        return ownerUserId;
    }

    public String getTitle() {
        return title;
    }

    public ConversationStatus getStatus() {
        return status;
    }

    public int getMessageCount() {
        return messageCount;
    }

    public String getLastMessagePreview() {
        return lastMessagePreview;
    }

    public Instant getLastMessageAt() {
        return lastMessageAt;
    }

    public List<ConversationMessageEntity> getMessages() {
        return messages;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    private String preview(String content) {
        String normalized = content.trim().replaceAll("\\s+", " ");
        if (normalized.length() <= 120) {
            return normalized;
        }
        return normalized.substring(0, 120);
    }
}
