package com.agentweave.conversation.application;

import com.agentweave.conversation.dto.ResponseMode;
import com.agentweave.conversation.dto.SendMessageRequest;
import com.agentweave.conversation.dto.SendMessageResponse;
import com.agentweave.shared.security.CurrentUser;
import com.agentweave.shared.security.CurrentUserService;
import com.agentweave.shared.tracing.CorrelationContext;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ChatApplicationService {

    private static final String SYNC_FAILURE_MESSAGE = "AI answer generation failed";
    private static final String PROVIDER = "openai";
    private static final String MODEL = "unknown";

    private final ConversationService conversationService;
    private final ConversationAiClient conversationAiClient;
    private final ConversationRagService conversationRagService;
    private final MessageMetadataService messageMetadataService;
    private final ModelCallLogService modelCallLogService;
    private final CurrentUserService currentUserService;
    private final CorrelationContext correlationContext;

    public ChatApplicationService(
            ConversationService conversationService,
            ConversationAiClient conversationAiClient,
            ConversationRagService conversationRagService,
            MessageMetadataService messageMetadataService,
            ModelCallLogService modelCallLogService,
            CurrentUserService currentUserService,
            CorrelationContext correlationContext) {
        this.conversationService = conversationService;
        this.conversationAiClient = conversationAiClient;
        this.conversationRagService = conversationRagService;
        this.messageMetadataService = messageMetadataService;
        this.modelCallLogService = modelCallLogService;
        this.currentUserService = currentUserService;
        this.correlationContext = correlationContext;
    }

    public SendMessageResponse sendMessage(UUID conversationId, SendMessageRequest request) {
        SendMessageResponse created = conversationService.sendMessage(conversationId, request);
        if (!ResponseMode.SYNC.equals(request.normalizedResponseMode())) {
            return created;
        }
        return completeSyncAnswer(conversationId, created);
    }

    private SendMessageResponse completeSyncAnswer(UUID conversationId, SendMessageResponse created) {
        CurrentUser user = currentUserService.requireCurrentUser();
        try (CorrelationContext.Scope ignored = correlationContext.open(
                created.traceId(),
                        conversationId,
                        created.assistantMessageId())) {
            ConversationPrompt basePrompt = conversationService.buildPrompt(conversationId, user.id());
            RagPromptContext ragContext = conversationRagService.retrieve(basePrompt);
            ConversationPrompt prompt = conversationService.withRagContext(basePrompt, ragContext);
            long startedAt = System.nanoTime();
            try {
                ConversationAiResponse response = conversationAiClient.answer(prompt);
                long latencyMs = elapsedMillis(startedAt);
                conversationService.completeAssistantMessage(
                        conversationId,
                        user.id(),
                        created.assistantMessageId(),
                        response.content(),
                        messageMetadataService.assistantRagMetadata(ragContext));
                modelCallLogService.recordSuccess(
                        conversationId,
                        created.assistantMessageId(),
                        response,
                        latencyMs,
                        created.traceId());
                return new SendMessageResponse(
                        created.conversationId(),
                        created.userMessageId(),
                        created.assistantMessageId(),
                        created.traceId(),
                        response.content(),
                        ragContext.retrievalMode(),
                        ragContext.citations(),
                        ragContext.graphPaths());
            } catch (RuntimeException ex) {
                long latencyMs = elapsedMillis(startedAt);
                conversationService.failAssistantMessage(
                        conversationId,
                        user.id(),
                        created.assistantMessageId(),
                        SYNC_FAILURE_MESSAGE);
                modelCallLogService.recordFailure(
                        conversationId,
                        created.assistantMessageId(),
                        PROVIDER,
                        MODEL,
                        latencyMs,
                        ex,
                        created.traceId());
                throw ex;
            }
        }
    }

    private long elapsedMillis(long startedAt) {
        return Duration.ofNanos(System.nanoTime() - startedAt).toMillis();
    }
}
