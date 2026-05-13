package com.agentweave.conversation.application;

import com.agentweave.conversation.domain.ConversationEntity;
import com.agentweave.conversation.domain.ConversationMessageEntity;
import com.agentweave.conversation.domain.MessageRole;
import com.agentweave.conversation.domain.MessageStatus;
import com.agentweave.shared.exception.ResourceNotFoundException;
import java.util.Comparator;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class MessageService {

    public ConversationMessageEntity createUserMessage(
            ConversationEntity conversation,
            UUID userId,
            String content,
            String traceId) {
        ConversationMessageEntity message = new ConversationMessageEntity(
                UUID.randomUUID(),
                userId,
                MessageRole.USER,
                content,
                MessageStatus.SUCCEEDED,
                traceId);
        conversation.addMessage(message);
        appendConversationSummary(conversation, message);
        return message;
    }

    public ConversationMessageEntity createPendingAssistantMessage(
            ConversationEntity conversation,
            UUID userId,
            String traceId) {
        ConversationMessageEntity message = new ConversationMessageEntity(
                UUID.randomUUID(),
                userId,
                MessageRole.ASSISTANT,
                "",
                MessageStatus.PENDING,
                traceId);
        conversation.addMessage(message);
        return message;
    }

    public UUID markAssistantStreaming(ConversationEntity conversation, UUID assistantMessageId) {
        ConversationMessageEntity assistantMessage = findAssistantMessage(conversation, assistantMessageId);
        assistantMessage.stream();
        return assistantMessage.getId();
    }

    public UUID markLatestAssistantStreaming(ConversationEntity conversation) {
        ConversationMessageEntity assistantMessage = findPendingOrStreamingAssistantMessage(conversation);
        assistantMessage.stream();
        return assistantMessage.getId();
    }

    public UUID completeAssistantMessage(
            ConversationEntity conversation,
            UUID assistantMessageId,
            String content) {
        ConversationMessageEntity assistantMessage = findAssistantMessage(conversation, assistantMessageId);
        assistantMessage.complete(content);
        appendConversationSummary(conversation, assistantMessage);
        return assistantMessage.getId();
    }

    public UUID completeAssistantMessage(
            ConversationEntity conversation,
            UUID assistantMessageId,
            String content,
            String metadata) {
        ConversationMessageEntity assistantMessage = findAssistantMessage(conversation, assistantMessageId);
        assistantMessage.complete(content);
        assistantMessage.replaceMetadata(metadata);
        appendConversationSummary(conversation, assistantMessage);
        return assistantMessage.getId();
    }

    public UUID completeLatestAssistantMessage(ConversationEntity conversation, String content) {
        ConversationMessageEntity assistantMessage = findPendingOrStreamingAssistantMessage(conversation);
        assistantMessage.complete(content);
        appendConversationSummary(conversation, assistantMessage);
        return assistantMessage.getId();
    }

    public UUID completeLatestAssistantMessage(ConversationEntity conversation, String content, String metadata) {
        ConversationMessageEntity assistantMessage = findPendingOrStreamingAssistantMessage(conversation);
        assistantMessage.complete(content);
        assistantMessage.replaceMetadata(metadata);
        appendConversationSummary(conversation, assistantMessage);
        return assistantMessage.getId();
    }

    public UUID failAssistantMessage(
            ConversationEntity conversation,
            UUID assistantMessageId,
            String errorCode,
            String message) {
        ConversationMessageEntity assistantMessage = findAssistantMessage(conversation, assistantMessageId);
        assistantMessage.fail(errorCode, message);
        appendConversationSummary(conversation, assistantMessage);
        return assistantMessage.getId();
    }

    public UUID failLatestAssistantMessage(ConversationEntity conversation, String errorCode, String message) {
        ConversationMessageEntity assistantMessage = findPendingOrStreamingAssistantMessage(conversation);
        assistantMessage.fail(errorCode, message);
        appendConversationSummary(conversation, assistantMessage);
        return assistantMessage.getId();
    }

    public UUID cancelAssistantMessage(
            ConversationEntity conversation,
            UUID assistantMessageId,
            String errorCode,
            String message) {
        ConversationMessageEntity assistantMessage = findAssistantMessage(conversation, assistantMessageId);
        assistantMessage.cancel(errorCode, message);
        appendConversationSummary(conversation, assistantMessage);
        return assistantMessage.getId();
    }

    public MessageStatus cancelActiveAssistantMessage(
            ConversationEntity conversation,
            UUID assistantMessageId,
            String errorCode,
            String message) {
        ConversationMessageEntity assistantMessage = findAssistantMessage(conversation, assistantMessageId);
        if (MessageStatus.PENDING.equals(assistantMessage.getStatus())
                || MessageStatus.STREAMING.equals(assistantMessage.getStatus())
                || MessageStatus.CANCELLED.equals(assistantMessage.getStatus())) {
            assistantMessage.cancel(errorCode, message);
            appendConversationSummary(conversation, assistantMessage);
        }
        return assistantMessage.getStatus();
    }

    public UUID cancelLatestAssistantMessage(ConversationEntity conversation, String errorCode, String message) {
        ConversationMessageEntity assistantMessage = findPendingOrStreamingAssistantMessage(conversation);
        assistantMessage.cancel(errorCode, message);
        appendConversationSummary(conversation, assistantMessage);
        return assistantMessage.getId();
    }

    public void appendConversationSummary(ConversationEntity conversation, ConversationMessageEntity message) {
        conversation.refreshSummaryFrom(message);
    }

    private ConversationMessageEntity findAssistantMessage(
            ConversationEntity conversation,
            UUID assistantMessageId) {
        return conversation.getMessages().stream()
                .filter(message -> assistantMessageId.equals(message.getId()))
                .filter(message -> MessageRole.ASSISTANT.equals(message.getRole()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("assistant message not found"));
    }

    private ConversationMessageEntity findPendingOrStreamingAssistantMessage(ConversationEntity conversation) {
        return conversation.getMessages().stream()
                .filter(message -> MessageRole.ASSISTANT.equals(message.getRole()))
                .filter(message -> MessageStatus.PENDING.equals(message.getStatus())
                        || MessageStatus.STREAMING.equals(message.getStatus()))
                .max(Comparator.comparing(ConversationMessageEntity::getCreatedAt))
                .orElseGet(() -> {
                    ConversationMessageEntity fallback = new ConversationMessageEntity(
                            UUID.randomUUID(),
                    conversation.getOwnerUserId(),
                    MessageRole.ASSISTANT,
                    "",
                    MessageStatus.PENDING,
                            null);
                    conversation.addMessage(fallback);
                    return fallback;
                });
    }
}
