package com.agentweave.conversation.application;

import com.agentweave.conversation.domain.MessageStatus;
import com.agentweave.conversation.domain.ModelCallScenario;
import com.agentweave.conversation.dto.CancelMessageResponse;
import com.agentweave.conversation.dto.ConversationMessageStatusResponse;
import com.agentweave.conversation.application.StreamTaskRegistry.StreamTask;
import com.agentweave.conversation.dto.WorkflowStepEventResponse;
import com.agentweave.observability.application.SseConnectionTracker;
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
    private static final String STREAM_ENDPOINT = "/api/v1/conversations/{conversationId}/stream";

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
    private final SseConnectionTracker sseConnectionTracker;

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
            CorrelationContext correlationContext,
            SseConnectionTracker sseConnectionTracker) {
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
        this.sseConnectionTracker = sseConnectionTracker;
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
        SseConnectionTracker.SseConnectionScope connectionScope = sseConnectionTracker.start(
                user.id(),
                conversationId,
                assistantMessageId,
                traceId,
                STREAM_ENDPOINT,
                MODEL);
        long startedAt = System.nanoTime();
        AtomicReference<StringBuilder> answer = new AtomicReference<>(new StringBuilder());
        AtomicReference<ConversationAiResponse> modelMetadata = new AtomicReference<>();
        AtomicBoolean terminalMessageStateUpdated = new AtomicBoolean(false);
        Flux<ServerSentEvent<?>> prefix = Flux.just(
                trackedEvent(connectionScope, "workflow_step", () ->
                        withCorrelation(traceId, conversationId, assistantMessageId, () ->
                                sseEventFactory.workflowStep(
                                        conversationId,
                                        assistantMessageId,
                                        new WorkflowStepEventResponse(
                                                UUID.randomUUID().toString(),
                                                "Planner",
                                                "SUCCEEDED",
                                                traceId),
                                        traceId))));
        Flux<ServerSentEvent<?>> citations = Flux.fromIterable(ragContext.citations())
                .map(citation -> trackedEvent(connectionScope, "citation", () ->
                        withCorrelation(traceId, conversationId, assistantMessageId, () ->
                                sseEventFactory.citation(
                                        conversationId,
                                        assistantMessageId,
                                        citation,
                                        traceId))));
        Flux<ServerSentEvent<?>> graphPaths = Flux.fromIterable(ragContext.graphPaths())
                .map(graphPath -> trackedEvent(connectionScope, "graph_path", () ->
                        withCorrelation(traceId, conversationId, assistantMessageId, () ->
                                sseEventFactory.graphPath(
                                        conversationId,
                                        assistantMessageId,
                                        graphPath,
                                        traceId))));
        Flux<ConversationAiChunk> answerStream = Flux.defer(() -> {
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
                .doOnNext(chunk -> {
                    if (chunk.hasMetadata()) {
                        modelMetadata.set(chunk.metadata());
                        sseConnectionTracker.modelResolved(connectionScope, modelName(chunk.metadata()));
                    }
                    if (chunk.hasContent()) {
                        answer.get().append(chunk.content());
                    }
                })
                .filter(ConversationAiChunk::hasContent)
                .map(chunk -> trackedEvent(connectionScope, "message_delta", () ->
                        withCorrelation(traceId, conversationId, assistantMessageId, () ->
                                sseEventFactory.messageDelta(
                                        conversationId,
                                        assistantMessageId,
                                        chunk.content(),
                                        traceId))));
        Flux<ServerSentEvent<?>> done = Flux.defer(() -> {
            UUID completedMessageId = withCorrelation(traceId, conversationId, assistantMessageId, () ->
                    conversationService.completePendingAssistantMessage(
                            conversationId,
                            user.id(),
                            answer.get().toString(),
                            messageMetadataService.assistantRagMetadata(ragContext)));
            terminalMessageStateUpdated.set(true);
            withCorrelation(traceId, conversationId, completedMessageId, () -> {
                modelCallLogService.recordStreamSuccess(
                        conversationId,
                        completedMessageId,
                        new ConversationAiResponse(
                                answer.get().toString(),
                                PROVIDER,
                                modelName(modelMetadata.get()),
                                promptTokens(modelMetadata.get()),
                                completionTokens(modelMetadata.get())),
                        promptSummary(prompt),
                        responseSummary(answer.get().toString()),
                        elapsedMillis(startedAt),
                        traceId,
                        scenario(ragContext, ModelCallScenario.CHAT_STREAM));
                return null;
            });
            ServerSentEvent<?> doneEvent = trackedEvent(connectionScope, "done", () ->
                    withCorrelation(traceId, conversationId, completedMessageId, () ->
                            sseEventFactory.done(conversationId, completedMessageId, traceId)));
            sseConnectionTracker.complete(connectionScope);
            return Flux.just(doneEvent);
        });
        Mono<StreamTermination> timeoutSignal = Mono.delay(chatProperties.streamTimeout())
                .map(ignored -> StreamTermination.timeout(STREAM_TIMEOUT_MESSAGE));
        Mono<StreamTermination> cancellationSignal = Mono.firstWithSignal(
                streamTask.cancellationSignal(),
                timeoutSignal);
        AtomicReference<StreamTermination> terminationRef = new AtomicReference<>();
        Flux<ServerSentEvent<?>> stream = Flux.concat(prefix, citations, graphPaths, deltas, done);
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
                                ragContext,
                                terminalMessageStateUpdated,
                                connectionScope);
                        }))
                .concatWith(Flux.defer(() -> {
                    StreamTermination termination = terminationRef.get();
                    if (termination == null || !StreamTerminationType.TIMEOUT.equals(termination.type())) {
                        return Flux.empty();
                    }
                    ServerSentEvent<?> errorEvent = trackedEvent(connectionScope, "error", () ->
                            withCorrelation(traceId, conversationId, assistantMessageId, () ->
                                    sseEventFactory.error(
                                            conversationId,
                                            assistantMessageId,
                                            termination.code(),
                                            termination.message(),
                                            traceId)));
                    sseConnectionTracker.timeout(connectionScope, termination.message());
                    return Flux.just(errorEvent);
                }))
                .onErrorResume(ex -> {
                    withCorrelation(traceId, conversationId, assistantMessageId, () -> {
                        conversationService.failPendingAssistantMessage(
                                conversationId,
                                user.id(),
                                STREAM_FAILURE_MESSAGE);
                        terminalMessageStateUpdated.set(true);
                        modelCallLogService.recordStreamFailure(
                                conversationId,
                                assistantMessageId,
                                PROVIDER,
                                MODEL,
                                scenario(ragContext, ModelCallScenario.CHAT_STREAM),
                                elapsedMillis(startedAt),
                                ex,
                                traceId);
                        return null;
                    });
                    ServerSentEvent<?> errorEvent = trackedEvent(connectionScope, "error", () ->
                            withCorrelation(traceId, conversationId, assistantMessageId, () ->
                                    sseEventFactory.error(
                                            conversationId,
                                            assistantMessageId,
                                            "SSE_STREAM_ERROR",
                                            STREAM_FAILURE_MESSAGE,
                                            traceId)));
                    sseConnectionTracker.fail(connectionScope, errorSummary(ex));
                    return Flux.just(errorEvent);
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
                            modelCallLogService.recordStreamCancelled(
                                    conversationId,
                                    assistantMessageId,
                                    PROVIDER,
                                    MODEL,
                                    scenario(ragContext, ModelCallScenario.CHAT_STREAM),
                                    elapsedMillis(startedAt),
                                    STREAM_CANCELLED_MESSAGE,
                                    traceId);
                            return null;
                        });
                        sseConnectionTracker.clientDisconnected(connectionScope);
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
            RagPromptContext ragContext,
            AtomicBoolean terminalMessageStateUpdated,
            SseConnectionTracker.SseConnectionScope connectionScope) {
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
                modelCallLogService.recordStreamTimeout(
                        conversationId,
                        assistantMessageId,
                        PROVIDER,
                        MODEL,
                        scenario(ragContext, ModelCallScenario.CHAT_STREAM),
                        elapsedMillis(startedAt),
                        new StreamTimeoutException(termination.message()),
                        traceId);
            } else {
                conversationService.cancelAssistantMessage(
                        conversationId,
                        userId,
                        assistantMessageId,
                        termination.message());
                modelCallLogService.recordStreamCancelled(
                        conversationId,
                        assistantMessageId,
                        PROVIDER,
                        MODEL,
                        scenario(ragContext, ModelCallScenario.CHAT_STREAM),
                        elapsedMillis(startedAt),
                        termination.message(),
                        traceId);
                sseConnectionTracker.cancel(connectionScope, termination.message());
            }
            return null;
        });
    }

    private long elapsedMillis(long startedAt) {
        return Duration.ofNanos(System.nanoTime() - startedAt).toMillis();
    }

    private ModelCallScenario scenario(RagPromptContext ragContext, ModelCallScenario fallback) {
        if (ragContext != null && (ragContext.hasCitations() || ragContext.hasGraphPaths())) {
            return ModelCallScenario.RAG_ANSWER;
        }
        return fallback;
    }

    private String promptSummary(ConversationPrompt prompt) {
        RagPromptContext ragContext = prompt.ragContext();
        int citations = ragContext == null ? 0 : ragContext.citations().size();
        int graphPaths = ragContext == null ? 0 : ragContext.graphPaths().size();
        String retrievalMode = ragContext == null ? "UNKNOWN" : ragContext.retrievalMode();
        int messageLength = prompt.latestUserMessage() == null ? 0 : prompt.latestUserMessage().length();
        return "conversationId=" + prompt.conversationId()
                + ";messageLength=" + messageLength
                + ";retrievalMode=" + retrievalMode
                + ";citations=" + citations
                + ";graphPaths=" + graphPaths;
    }

    private String responseSummary(String content) {
        return "contentLength=" + (content == null ? 0 : content.length());
    }

    private String errorSummary(Throwable ex) {
        if (ex == null) {
            return STREAM_FAILURE_MESSAGE;
        }
        String message = ex.getMessage();
        return message == null || message.isBlank() ? ex.getClass().getSimpleName() : message;
    }

    private String modelName(ConversationAiResponse response) {
        if (response == null || response.model() == null || response.model().isBlank()) {
            return MODEL;
        }
        return response.model();
    }

    private Integer promptTokens(ConversationAiResponse response) {
        return response == null ? null : response.promptTokens();
    }

    private Integer completionTokens(ConversationAiResponse response) {
        return response == null ? null : response.completionTokens();
    }

    private <T> T withCorrelation(String traceId, UUID conversationId, UUID messageId, CorrelatedAction<T> action) {
        try (CorrelationContext.Scope ignored = correlationContext.open(traceId, conversationId, messageId)) {
            return action.execute();
        }
    }

    private ServerSentEvent<?> trackedEvent(
            SseConnectionTracker.SseConnectionScope connectionScope,
            String eventType,
            CorrelatedAction<ServerSentEvent<?>> action) {
        ServerSentEvent<?> event = action.execute();
        sseConnectionTracker.recordEvent(connectionScope, eventType);
        return event;
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
