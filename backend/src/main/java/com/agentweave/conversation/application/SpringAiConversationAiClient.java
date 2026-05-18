package com.agentweave.conversation.application;

import com.agentweave.shared.tracing.CorrelationContext;
import com.agentweave.shared.tracing.TraceIdProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SignalType;

@Service
public class SpringAiConversationAiClient implements ConversationAiClient {

    private static final Logger log = LoggerFactory.getLogger(SpringAiConversationAiClient.class);
    private static final String SYSTEM_PROMPT = """
            你是 AgentWeave Enterprise 的企业级 AI Agent 助手。
            你需要用中文回答，优先给出可执行、可验证的工程化建议。
            你会收到受控的知识库引用上下文；回答必须优先基于这些资料和会话上下文。
            如果知识库没有召回相关资料，或资料不足以确认结论，请明确说明当前资料无法确认。
            不要编造引用、工具结果、内部日志或不存在的数据。
            """;
    private static final String PROVIDER = "openai";
    private static final String DEFAULT_MODEL = "unknown";

    private final ChatClient chatClient;
    private final ConversationMemoryService conversationMemoryService;

    public SpringAiConversationAiClient(
            ChatClient.Builder chatClientBuilder,
            MessageChatMemoryAdvisor messageChatMemoryAdvisor,
            ConversationMemoryService conversationMemoryService) {
        this.chatClient = chatClientBuilder
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(messageChatMemoryAdvisor)
                .build();
        this.conversationMemoryService = conversationMemoryService;
    }

    @Override
    public ConversationAiResponse answer(ConversationPrompt prompt) {
        conversationMemoryService.syncBeforeModelCall(prompt);
        ChatResponse chatResponse = chatClient.prompt()
                .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, prompt.conversationId().toString()))
                .user(buildUserPrompt(prompt))
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        ChatResponseMetadata metadata = chatResponse.getMetadata();
        Usage usage = metadata == null ? null : metadata.getUsage();
        return new ConversationAiResponse(
                content,
                PROVIDER,
                model(metadata),
                promptTokens(usage),
                completionTokens(usage));
    }

    @Override
    public Flux<ConversationAiChunk> streamAnswer(ConversationPrompt prompt) {
        conversationMemoryService.syncBeforeModelCall(prompt);
        String traceId = MDC.get(TraceIdProvider.TRACE_ID_KEY);
        String conversationId = prompt.conversationId().toString();
        String messageId = MDC.get(CorrelationContext.MESSAGE_ID_KEY);
        return chatClient.prompt()
                .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, prompt.conversationId().toString()))
                .user(buildUserPrompt(prompt))
                .stream()
                .chatResponse()
                .doOnSubscribe(ignored -> log.info(
                        "Provider stream subscribed: traceId={}, conversationId={}, messageId={}",
                        traceId,
                        conversationId,
                        messageId))
                .doOnCancel(() -> log.info(
                        "Provider stream cancelled: traceId={}, conversationId={}, messageId={}",
                        traceId,
                        conversationId,
                        messageId))
                .doFinally(signalType -> logProviderStreamFinished(traceId, conversationId, messageId, signalType))
                .map(this::toChunk)
                .filter(chunk -> chunk.hasContent() || chunk.hasMetadata());
    }

    private ConversationAiChunk toChunk(ChatResponse chatResponse) {
        String content = content(chatResponse);
        ConversationAiResponse metadata = responseMetadata(chatResponse);
        return new ConversationAiChunk(content, metadata);
    }

    private String content(ChatResponse chatResponse) {
        if (chatResponse == null || chatResponse.getResult() == null) {
            return "";
        }
        Generation generation = chatResponse.getResult();
        if (generation.getOutput() == null || generation.getOutput().getText() == null) {
            return "";
        }
        return generation.getOutput().getText();
    }

    private ConversationAiResponse responseMetadata(ChatResponse chatResponse) {
        if (chatResponse == null || chatResponse.getMetadata() == null) {
            return null;
        }
        ChatResponseMetadata metadata = chatResponse.getMetadata();
        Usage usage = metadata.getUsage();
        if ((metadata.getModel() == null || metadata.getModel().isBlank()) && usage == null) {
            return null;
        }
        return new ConversationAiResponse(
                "",
                PROVIDER,
                model(metadata),
                promptTokens(usage),
                completionTokens(usage));
    }

    private String buildUserPrompt(ConversationPrompt prompt) {
        return """
                会话标题：%s

                知识库上下文：
                %s

                回答约束：
                - 优先基于知识库上下文回答。
                - 引用资料只能来自上方列出的 documentId/chunkId，不要伪造来源。
                - 如果没有相关引用或引用不足，请说明当前知识库资料不足。

                用户消息：
                %s
                """.formatted(
                prompt.title(),
                prompt.ragContext().promptContext(),
                prompt.latestUserMessage());
    }

    private String model(ChatResponseMetadata metadata) {
        if (metadata == null || metadata.getModel() == null || metadata.getModel().isBlank()) {
            return DEFAULT_MODEL;
        }
        return metadata.getModel();
    }

    private Integer promptTokens(Usage usage) {
        return usage == null ? null : usage.getPromptTokens();
    }

    private Integer completionTokens(Usage usage) {
        return usage == null ? null : usage.getCompletionTokens();
    }

    private void logProviderStreamFinished(
            String traceId,
            String conversationId,
            String messageId,
            SignalType signalType) {
        if (SignalType.CANCEL.equals(signalType)) {
            return;
        }
        log.info(
                "Provider stream finished: traceId={}, conversationId={}, messageId={}, signal={}",
                traceId,
                conversationId,
                messageId,
                signalType);
    }
}
