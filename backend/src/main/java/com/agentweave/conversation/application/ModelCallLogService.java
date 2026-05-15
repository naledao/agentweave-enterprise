package com.agentweave.conversation.application;

import com.agentweave.conversation.domain.ModelCallLogEntity;
import com.agentweave.conversation.domain.ModelCallStatus;
import com.agentweave.conversation.repository.ModelCallLogRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ModelCallLogService {

    private static final int ERROR_MESSAGE_MAX_LENGTH = 500;
    private static final String DEFAULT_PROVIDER = "openai";
    private static final String DEFAULT_MODEL = "unknown";
    private static final String MODEL_CALL_FAILED = "MODEL_CALL_FAILED";

    private final ModelCallLogRepository modelCallLogRepository;

    public ModelCallLogService(ModelCallLogRepository modelCallLogRepository) {
        this.modelCallLogRepository = modelCallLogRepository;
    }

    @Transactional
    public void recordSuccess(
            UUID conversationId,
            UUID messageId,
            ConversationAiResponse response,
            long latencyMs,
            String traceId) {
        modelCallLogRepository.save(new ModelCallLogEntity(
                UUID.randomUUID(),
                conversationId,
                messageId,
                normalize(response.provider(), DEFAULT_PROVIDER),
                normalize(response.model(), DEFAULT_MODEL),
                response.promptTokens(),
                response.completionTokens(),
                latencyMs,
                ModelCallStatus.SUCCEEDED,
                null,
                null,
                traceId));
    }

    @Transactional
    public void recordFailure(
            UUID conversationId,
            UUID messageId,
            String provider,
            String model,
            long latencyMs,
            Throwable failure,
            String traceId) {
        modelCallLogRepository.save(new ModelCallLogEntity(
                UUID.randomUUID(),
                conversationId,
                messageId,
                normalize(provider, DEFAULT_PROVIDER),
                normalize(model, DEFAULT_MODEL),
                null,
                null,
                latencyMs,
                ModelCallStatus.FAILED,
                MODEL_CALL_FAILED,
                sanitize(failure),
                traceId));
    }

    private String normalize(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    private String sanitize(Throwable failure) {
        String message = failure.getMessage();
        if (message == null || message.isBlank()) {
            message = failure.getClass().getSimpleName();
        }
        String sanitized = message.replaceAll("(?i)(api[-_ ]?key|token|secret|password)=\\S+", "$1=******");
        if (sanitized.length() <= ERROR_MESSAGE_MAX_LENGTH) {
            return sanitized;
        }
        return sanitized.substring(0, ERROR_MESSAGE_MAX_LENGTH);
    }

    @Transactional
    public void recordAgentCall(
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
            String traceId,
            String agentStage,
            UUID agentRunId,
            UUID agentStepId) {
        modelCallLogRepository.save(new ModelCallLogEntity(
                UUID.randomUUID(),
                conversationId,
                messageId,
                normalize(provider, DEFAULT_PROVIDER),
                normalize(model, DEFAULT_MODEL),
                promptTokens,
                completionTokens,
                latencyMs,
                status,
                errorCode,
                errorMessage,
                traceId,
                agentStage,
                agentRunId,
                agentStepId));
    }
}
