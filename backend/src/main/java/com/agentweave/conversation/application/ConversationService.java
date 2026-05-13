package com.agentweave.conversation.application;

import com.agentweave.conversation.domain.ConversationEntity;
import com.agentweave.conversation.domain.ConversationMessageEntity;
import com.agentweave.conversation.domain.ConversationStatus;
import com.agentweave.conversation.domain.MessageRole;
import com.agentweave.conversation.domain.MessageStatus;
import com.agentweave.conversation.dto.ConversationDetailResponse;
import com.agentweave.conversation.dto.ConversationListResponse;
import com.agentweave.conversation.dto.ConversationMessageQueryRequest;
import com.agentweave.conversation.dto.ConversationMessageResponse;
import com.agentweave.conversation.dto.ConversationQueryRequest;
import com.agentweave.conversation.dto.ConversationSummaryResponse;
import com.agentweave.conversation.dto.CreateConversationRequest;
import com.agentweave.conversation.dto.ConversationResponse;
import com.agentweave.conversation.dto.SendMessageRequest;
import com.agentweave.conversation.dto.SendMessageResponse;
import com.agentweave.conversation.repository.ConversationMessageRepository;
import com.agentweave.conversation.repository.ConversationRepository;
import com.agentweave.shared.exception.ResourceNotFoundException;
import com.agentweave.shared.security.CurrentUser;
import com.agentweave.shared.security.CurrentUserService;
import com.agentweave.shared.tracing.TraceIdProvider;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConversationService {

    private static final String DEFAULT_TITLE = "新的对话";

    private final ConversationRepository conversationRepository;
    private final ConversationMessageRepository conversationMessageRepository;
    private final MessageService messageService;
    private final CurrentUserService currentUserService;
    private final TraceIdProvider traceIdProvider;

    public ConversationService(
            ConversationRepository conversationRepository,
            ConversationMessageRepository conversationMessageRepository,
            MessageService messageService,
            CurrentUserService currentUserService,
            TraceIdProvider traceIdProvider) {
        this.conversationRepository = conversationRepository;
        this.conversationMessageRepository = conversationMessageRepository;
        this.messageService = messageService;
        this.currentUserService = currentUserService;
        this.traceIdProvider = traceIdProvider;
    }

    @Transactional
    public ConversationResponse create(CreateConversationRequest request) {
        CurrentUser user = currentUserService.requireCurrentUser();
        String traceId = traceIdProvider.currentTraceId();
        ConversationEntity conversation = new ConversationEntity(
                UUID.randomUUID(),
                user.id(),
                normalizeTitle(request.title()));
        return ConversationResponse.from(conversationRepository.saveAndFlush(conversation), traceId);
    }

    @Transactional(readOnly = true)
    public ConversationListResponse list(ConversationQueryRequest request) {
        CurrentUser user = currentUserService.requireCurrentUser();
        Pageable pageable = PageRequest.of(
                request.pageNumber(),
                request.pageSize(),
                Sort.by(Sort.Direction.DESC, "updatedAt"));
        String keyword = request.normalizedKeyword();
        Page<ConversationEntity> page = keyword == null
                ? conversationRepository.findOwnedConversations(user.id(), ConversationStatus.DELETED, pageable)
                : conversationRepository.searchOwnedConversations(user.id(), ConversationStatus.DELETED, keyword, pageable);
        Page<ConversationSummaryResponse> conversations = page
                .map(ConversationSummaryResponse::from);
        return ConversationListResponse.from(conversations);
    }

    @Transactional(readOnly = true)
    public ConversationDetailResponse get(UUID conversationId, ConversationMessageQueryRequest request) {
        CurrentUser user = currentUserService.requireCurrentUser();
        ConversationEntity conversation = findOwnedConversationSummary(conversationId, user);
        Pageable pageable = PageRequest.of(
                request.pageNumber(),
                request.pageSize());
        Page<ConversationMessageResponse> messages = conversationMessageRepository
                .findConversationMessagesAsc(conversationId, pageable)
                .map(ConversationMessageResponse::from);
        return ConversationDetailResponse.from(conversation, messages, traceIdProvider.currentTraceId());
    }

    @Transactional
    public SendMessageResponse sendMessage(UUID conversationId, SendMessageRequest request) {
        CurrentUser user = currentUserService.requireCurrentUser();
        String traceId = traceIdProvider.currentTraceId();
        ConversationEntity conversation = findOwnedConversation(conversationId, user);
        ConversationMessageEntity userMessage = messageService.createUserMessage(
                conversation,
                user.id(),
                request.content().trim(),
                traceId);
        ConversationMessageEntity assistantMessage = messageService.createPendingAssistantMessage(
                conversation,
                user.id(),
                traceId);
        messageService.appendConversationSummary(conversation, userMessage);
        if (conversation.getMessages().size() == 2 && DEFAULT_TITLE.equals(conversation.getTitle())) {
            conversation.rename(toTitle(request.content()));
        }
        return new SendMessageResponse(conversation.getId(), userMessage.getId(), assistantMessage.getId(), traceId);
    }

    @Transactional
    public UUID appendAssistantMessage(UUID conversationId, String content, MessageStatus status) {
        CurrentUser user = currentUserService.requireCurrentUser();
        return appendAssistantMessage(conversationId, user.id(), content, status);
    }

    @Transactional
    public UUID appendAssistantMessage(UUID conversationId, UUID ownerUserId, String content, MessageStatus status) {
        ConversationEntity conversation = findOwnedConversation(conversationId, ownerUserId);
        ConversationMessageEntity assistantMessage = messageService.createPendingAssistantMessage(
                conversation,
                ownerUserId,
                traceIdProvider.currentTraceId());
        if (MessageStatus.STREAMING.equals(status)) {
            assistantMessage.stream();
        } else if (MessageStatus.SUCCEEDED.equals(status)) {
            assistantMessage.complete(content);
            messageService.appendConversationSummary(conversation, assistantMessage);
        } else if (MessageStatus.FAILED.equals(status)) {
            assistantMessage.fail("CHAT_ASSISTANT_FAILED", content);
            messageService.appendConversationSummary(conversation, assistantMessage);
        } else if (MessageStatus.CANCELLED.equals(status)) {
            assistantMessage.cancel("CHAT_ASSISTANT_CANCELLED", content);
            messageService.appendConversationSummary(conversation, assistantMessage);
        }
        return assistantMessage.getId();
    }

    @Transactional
    public UUID completePendingAssistantMessage(UUID conversationId, UUID ownerUserId, String content) {
        ConversationEntity conversation = findOwnedConversation(conversationId, ownerUserId);
        return messageService.completeLatestAssistantMessage(conversation, content);
    }

    @Transactional
    public UUID startPendingAssistantMessage(UUID conversationId, UUID ownerUserId) {
        ConversationEntity conversation = findOwnedConversation(conversationId, ownerUserId);
        return messageService.markLatestAssistantStreaming(conversation);
    }

    @Transactional
    public void completeAssistantMessage(
            UUID conversationId,
            UUID ownerUserId,
            UUID assistantMessageId,
            String content) {
        ConversationEntity conversation = findOwnedConversation(conversationId, ownerUserId);
        messageService.completeAssistantMessage(conversation, assistantMessageId, content);
    }

    @Transactional
    public void failPendingAssistantMessage(UUID conversationId, UUID ownerUserId, String message) {
        ConversationEntity conversation = findOwnedConversation(conversationId, ownerUserId);
        messageService.failLatestAssistantMessage(conversation, "CHAT_ASSISTANT_FAILED", message);
    }

    @Transactional
    public void failAssistantMessage(
            UUID conversationId,
            UUID ownerUserId,
            UUID assistantMessageId,
            String message) {
        ConversationEntity conversation = findOwnedConversation(conversationId, ownerUserId);
        messageService.failAssistantMessage(
                conversation,
                assistantMessageId,
                "CHAT_ASSISTANT_FAILED",
                message);
    }

    @Transactional
    public void failAssistantMessage(
            UUID conversationId,
            UUID ownerUserId,
            UUID assistantMessageId,
            String errorCode,
            String message) {
        ConversationEntity conversation = findOwnedConversation(conversationId, ownerUserId);
        messageService.failAssistantMessage(
                conversation,
                assistantMessageId,
                errorCode,
                message);
    }

    @Transactional
    public void cancelAssistantMessage(
            UUID conversationId,
            UUID ownerUserId,
            UUID assistantMessageId,
            String message) {
        ConversationEntity conversation = findOwnedConversation(conversationId, ownerUserId);
        messageService.cancelAssistantMessage(
                conversation,
                assistantMessageId,
                "CHAT_ASSISTANT_CANCELLED",
                message);
    }

    @Transactional
    public MessageStatus cancelActiveAssistantMessage(
            UUID conversationId,
            UUID ownerUserId,
            UUID assistantMessageId,
            String errorCode,
            String message) {
        ConversationEntity conversation = findOwnedConversation(conversationId, ownerUserId);
        return messageService.cancelActiveAssistantMessage(
                conversation,
                assistantMessageId,
                errorCode,
                message);
    }

    @Transactional(readOnly = true)
    public ConversationPrompt buildPrompt(UUID conversationId, UUID ownerUserId) {
        ConversationEntity conversation = findOwnedConversation(conversationId, ownerUserId);
        List<ConversationTurn> turns = conversation.getMessages().stream()
                .sorted(Comparator.comparing(ConversationMessageEntity::getCreatedAt))
                .filter(message -> MessageStatus.SUCCEEDED.equals(message.getStatus()))
                .map(message -> new ConversationTurn(message.getRole().name(), message.getContent()))
                .toList();
        String latestUserMessage = conversation.getMessages().stream()
                .filter(message -> MessageRole.USER.equals(message.getRole()))
                .max(Comparator.comparing(ConversationMessageEntity::getCreatedAt))
                .map(ConversationMessageEntity::getContent)
                .orElse("");
        return new ConversationPrompt(conversation.getId(), conversation.getTitle(), latestUserMessage, turns);
    }

    private ConversationEntity findOwnedConversation(UUID conversationId, CurrentUser user) {
        return findOwnedConversation(conversationId, user.id());
    }

    private ConversationEntity findOwnedConversation(UUID conversationId, UUID ownerUserId) {
        return conversationRepository.findWithMessagesByIdAndOwnerUserId(conversationId, ownerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("conversation not found"));
    }

    private ConversationEntity findOwnedConversationSummary(UUID conversationId, CurrentUser user) {
        return conversationRepository.findByIdAndOwnerUserId(conversationId, user.id())
                .orElseThrow(() -> new ResourceNotFoundException("conversation not found"));
    }

    private String normalizeTitle(String title) {
        if (title == null || title.isBlank()) {
            return DEFAULT_TITLE;
        }
        return title.trim();
    }

    private String toTitle(String content) {
        String trimmed = content.trim();
        if (trimmed.length() <= 32) {
            return trimmed;
        }
        return trimmed.substring(0, 32);
    }
}
