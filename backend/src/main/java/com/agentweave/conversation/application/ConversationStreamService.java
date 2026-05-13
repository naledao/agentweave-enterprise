package com.agentweave.conversation.application;

import com.agentweave.conversation.domain.MessageStatus;
import com.agentweave.conversation.dto.CancelMessageResponse;
import com.agentweave.conversation.dto.ConversationMessageStatusResponse;
import com.agentweave.conversation.application.StreamTaskRegistry.StreamTask;
import com.agentweave.conversation.dto.WorkflowStepEventResponse;
import com.agentweave.shared.security.CurrentUser;
import com.agentweave.shared.security.CurrentUserService;
import com.agentweave.shared.tracing.CorrelationContext;
import com.agentweave.shared.tracing.TraceIdProvider;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;

@Service
public class ConversationStreamService {

    private static final String STREAM_FAILURE_MESSAGE = "SSE stream failed";
    private static final String STREAM_CANCELLED_MESSAGE = "SSE stream cancelled";
    private static final String STREAM_TIMEOUT_MESSAGE = "SSE stream timed out";
    private static final String PROVIDER = "openai";
    private static final String MODEL = "unknown";

    private final ConversationService conversationService;
    private final ConversationAiClient conversationAiClient;
    private final ConversationRagService conversationRagService;
    private final MessageMetadataService messageMetadataService;
    private final ModelCallLogService modelCallLogService;
    private final SseEventFactory sseEventFactory;
    private final StreamTaskRegistry streamTaskRegistry;
    private final ChatProperties chatProperties;
    private final CurrentUserService currentUserService;
    private final TraceIdProvider traceIdProvider;
    private final CorrelationContext correlationContext;

    public ConversationStreamService(
            ConversationService conversationService,
            ConversationAiClient conversationAiClient,
            ConversationRagService conversationRagService,
            MessageMetadataService messageMetadataService,
            ModelCallLogService modelCallLogService,
            SseEventFactory sseEventFactory,
            StreamTaskRegistry streamTaskRegistry,
            ChatProperties chatProperties,
            CurrentUserService currentUserService,
            TraceIdProvider traceIdProvider,
            CorrelationContext correlationContext) {
        this.conversationService = conversationService;
        this.conversationAiClient = conversationAiClient;
        this.conversationRagService = conversationRagService;
        this.messageMetadataService = messageMetadataService;
        this.modelCallLogService = modelCallLogService;
        this.sseEventFactory = sseEventFactory;
        this.streamTaskRegistry = streamTaskRegistry;
        this.chatProperties = chatProperties;
        this.currentUserService = currentUserService;
        this.traceIdProvider = traceIdProvider;
        this.correlationContext = correlationContext;
    }

    public Flux<ServerSentEvent<?>> stream(UUID conversationId) {
        CurrentUser user = currentUserService.requireCurrentUser();
        String traceId = traceIdProvider.currentTraceId();
        streamTaskRegistry.assertCanRegister(user.id());
        UUID assistantMessageId = conversationService.startPendingAssistantMessage(conversationId, user.id());
        StreamTask streamTask = streamTaskRegistry.register(user.id(), conversationId, assistantMessageId, traceId);
        ConversationPrompt basePrompt = conversationService.buildPrompt(conversationId, user.id());
        RagPromptContext ragContext = withCorrelation(traceId, conversationId, assistantMessageId, () ->
                conversationRagService.retrieve(basePrompt));
        ConversationPrompt prompt = conversationService.withRagContext(basePrompt, ragContext);
        long startedAt = System.nanoTime();
        AtomicReference<StringBuilder> answer = new AtomicReference<>(new StringBuilder());
        AtomicBoolean terminalMessageStateUpdated = new AtomicBoolean(false);
        Flux<ServerSentEvent<?>> prefix = Flux.just(
                withCorrelation(traceId, conversationId, assistantMessageId, () ->
                        sseEventFactory.workflowStep(
                                conversationId,
                                assistantMessageId,
                                new WorkflowStepEventResponse(
                                        UUID.randomUUID().toString(),
                                        "Planner",
                                        "SUCCEEDED",
                                traceId),
                        traceId)));
        Flux<ServerSentEvent<?>> citations = Flux.fromIterable(ragContext.citations())
                .map(citation -> withCorrelation(traceId, conversationId, assistantMessageId, () ->
                        sseEventFactory.citation(
                                conversationId,
                                assistantMessageId,
                                citation,
                                traceId)));
        Flux<String> answerStream = Flux.defer(() -> {
            CorrelationContext.Scope scope = correlationContext.open(traceId, conversationId, assistantMessageId);
            try {
                return conversationAiClient.streamAnswer(prompt)
                        .doFinally(ignored -> scope.close());
            } catch (RuntimeException ex) {
                scope.close();
                return Flux.error(ex);
            }
        });
        Flux<ServerSentEvent<?>> deltas = answerStream
                .doOnNext(chunk -> answer.get().append(chunk))
                .map(chunk -> withCorrelation(traceId, conversationId, assistantMessageId, () ->
                        sseEventFactory.messageDelta(
                                conversationId,
                                assistantMessageId,
                                chunk,
                                traceId)));
        Flux<ServerSentEvent<?>> done = Flux.defer(() -> {
            UUID completedMessageId = withCorrelation(traceId, conversationId, assistantMessageId, () ->
                    conversationService.completePendingAssistantMessage(
                            conversationId,
                            user.id(),
                            answer.get().toString(),
                            messageMetadataService.assistantRagMetadata(ragContext)));
            terminalMessageStateUpdated.set(true);
            withCorrelation(traceId, conversationId, completedMessageId, () -> {
                modelCallLogService.recordSuccess(
                        conversationId,
                        completedMessageId,
                        new ConversationAiResponse(
                                answer.get().toString(),
                                PROVIDER,
                                MODEL,
                                null,
                                null),
                        elapsedMillis(startedAt),
                        traceId);
                return null;
            });
            return Flux.just(withCorrelation(traceId, conversationId, completedMessageId, () ->
                    sseEventFactory.done(conversationId, completedMessageId, traceId)));
        });
        Mono<StreamTermination> timeoutSignal = Mono.delay(chatProperties.streamTimeout())
                .map(ignored -> StreamTermination.timeout(STREAM_TIMEOUT_MESSAGE));
        Mono<StreamTermination> cancellationSignal = Mono.firstWithSignal(
                streamTask.cancellationSignal(),
                timeoutSignal);
        AtomicReference<StreamTermination> terminationRef = new AtomicReference<>();
        Flux<ServerSentEvent<?>> stream = Flux.concat(prefix, citations, deltas, done);
        return stream.takeUntilOther(cancellationSignal.doOnNext(termination ->
                        {
                            terminationRef.set(termination);
                            handleTermination(
                                termination,
                                conversationId,
                                user.id(),
                                assistantMessageId,
                                traceId,
                                startedAt,
                                terminalMessageStateUpdated);
                        }))
                .concatWith(Flux.defer(() -> {
                    StreamTermination termination = terminationRef.get();
                    if (termination == null || !StreamTerminationType.TIMEOUT.equals(termination.type())) {
                        return Flux.empty();
                    }
                    return Flux.just(withCorrelation(traceId, conversationId, assistantMessageId, () ->
                            sseEventFactory.error(
                                    conversationId,
                                    assistantMessageId,
                                    termination.code(),
                                    termination.message(),
                                    traceId)));
                }))
                .onErrorResume(ex -> {
                    withCorrelation(traceId, conversationId, assistantMessageId, () -> {
                        conversationService.failPendingAssistantMessage(
                                conversationId,
                                user.id(),
                                STREAM_FAILURE_MESSAGE);
                        terminalMessageStateUpdated.set(true);
                        modelCallLogService.recordFailure(
                                conversationId,
                                assistantMessageId,
                                PROVIDER,
                                MODEL,
                                elapsedMillis(startedAt),
                                ex,
                                traceId);
                        return null;
                    });
                    return Flux.just(withCorrelation(traceId, conversationId, assistantMessageId, () ->
                            sseEventFactory.error(
                                    conversationId,
                                    assistantMessageId,
                                    "SSE_STREAM_ERROR",
                                    STREAM_FAILURE_MESSAGE,
                                    traceId)));
                })
                .doFinally(signalType -> {
                    if (SignalType.CANCEL.equals(signalType)
                            && terminalMessageStateUpdated.compareAndSet(false, true)) {
                        withCorrelation(traceId, conversationId, assistantMessageId, () -> {
                            conversationService.cancelAssistantMessage(
                                    conversationId,
                                    user.id(),
                                    assistantMessageId,
                                    STREAM_CANCELLED_MESSAGE);
                            return null;
                        });
                    }
                    streamTaskRegistry.remove(streamTask);
                });
    }

    public CancelMessageResponse cancel(UUID conversationId, UUID messageId) {
        CurrentUser user = currentUserService.requireCurrentUser();
        StreamTermination termination = StreamTermination.cancelled(STREAM_CANCELLED_MESSAGE);
        String traceId = streamTaskRegistry.cancel(user.id(), conversationId, messageId, termination)
                .map(StreamTaskRegistry.StreamTaskSnapshot::traceId)
                .orElseGet(traceIdProvider::currentTraceId);
        MessageStatus status = withCorrelation(traceId, conversationId, messageId, () ->
                conversationService.cancelActiveAssistantMessage(
                        conversationId,
                        user.id(),
                        messageId,
                        termination.code(),
                        termination.message()));
        return new CancelMessageResponse(
                conversationId,
                messageId,
                ConversationMessageStatusResponse.from(status),
                traceId);
    }

    private void handleTermination(
            StreamTermination termination,
            UUID conversationId,
            UUID userId,
            UUID assistantMessageId,
            String traceId,
            long startedAt,
            AtomicBoolean terminalMessageStateUpdated) {
        if (!terminalMessageStateUpdated.compareAndSet(false, true)) {
            return;
        }
        withCorrelation(traceId, conversationId, assistantMessageId, () -> {
            if (StreamTerminationType.TIMEOUT.equals(termination.type())) {
                conversationService.failAssistantMessage(
                        conversationId,
                        userId,
                        assistantMessageId,
                        termination.code(),
                        termination.message());
                modelCallLogService.recordFailure(
                        conversationId,
                        assistantMessageId,
                        PROVIDER,
                        MODEL,
                        elapsedMillis(startedAt),
                        new StreamTimeoutException(termination.message()),
                        traceId);
            } else {
                conversationService.cancelAssistantMessage(
                        conversationId,
                        userId,
                        assistantMessageId,
                        termination.message());
            }
            return null;
        });
    }

    private long elapsedMillis(long startedAt) {
        return Duration.ofNanos(System.nanoTime() - startedAt).toMillis();
    }

    private <T> T withCorrelation(String traceId, UUID conversationId, UUID messageId, CorrelatedAction<T> action) {
        try (CorrelationContext.Scope ignored = correlationContext.open(traceId, conversationId, messageId)) {
            return action.execute();
        }
    }

    @FunctionalInterface
    private interface CorrelatedAction<T> {

        T execute();
    }

    private static class StreamTimeoutException extends RuntimeException {

        StreamTimeoutException(String message) {
            super(message);
        }
    }
}
