package com.agentweave.conversation.application;

import com.agentweave.conversation.domain.ModelCallLogEntity;
import com.agentweave.conversation.domain.ModelCallScenario;
import com.agentweave.conversation.domain.ModelCallStatus;
import com.agentweave.conversation.repository.ModelCallLogRepository;
import com.agentweave.observability.application.AgentWeaveMetrics;
import com.agentweave.shared.audit.AuditEventType;
import com.agentweave.shared.audit.AuditLogCommand;
import com.agentweave.shared.audit.AuditLogService;
import com.agentweave.shared.audit.AuditResult;
import com.agentweave.shared.audit.AuditSummarySanitizer;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ModelCallLogService {

    private static final int ERROR_MESSAGE_MAX_LENGTH = 500;
    private static final int SUMMARY_MAX_LENGTH = 1000;
    private static final String DEFAULT_PROVIDER = "openai";
    private static final String DEFAULT_MODEL = "unknown";
    private static final String MODEL_CALL_FAILED = "MODEL_CALL_FAILED";
    private static final String MODEL_CALL_TIMEOUT = "MODEL_CALL_TIMEOUT";
    private static final String MODEL_CALL_CANCELLED = "MODEL_CALL_CANCELLED";

    private final ModelCallLogRepository modelCallLogRepository;
    private final AuditLogService auditLogService;
    private final AuditSummarySanitizer auditSummarySanitizer;
    private final AgentWeaveMetrics agentWeaveMetrics;

    public ModelCallLogService(
            ModelCallLogRepository modelCallLogRepository,
            AuditLogService auditLogService,
            AuditSummarySanitizer auditSummarySanitizer,
            AgentWeaveMetrics agentWeaveMetrics) {
        this.modelCallLogRepository = modelCallLogRepository;
        this.auditLogService = auditLogService;
        this.auditSummarySanitizer = auditSummarySanitizer;
        this.agentWeaveMetrics = agentWeaveMetrics;
    }

    @Transactional
    public void recordSuccess(
            UUID conversationId,
            UUID messageId,
            ConversationAiResponse response,
            String promptSummary,
            String responseSummary,
            long latencyMs,
            String traceId,
            ModelCallScenario scenario) {
        recordCall(
                conversationId,
                messageId,
                normalize(response.provider(), DEFAULT_PROVIDER),
                normalize(response.model(), DEFAULT_MODEL),
                normalizeScenario(scenario, ModelCallScenario.CHAT_SYNC),
                promptSummary,
                responseSummary,
                response.promptTokens(),
                response.completionTokens(),
                latencyMs,
                ModelCallStatus.SUCCESS,
                null,
                null,
                traceId,
                null,
                null,
                null,
                false);
    }

    @Transactional
    public void recordStreamSuccess(
            UUID conversationId,
            UUID messageId,
            ConversationAiResponse response,
            String promptSummary,
            String responseSummary,
            long latencyMs,
            String traceId,
            ModelCallScenario scenario) {
        recordCall(
                conversationId,
                messageId,
                normalize(response.provider(), DEFAULT_PROVIDER),
                normalize(response.model(), DEFAULT_MODEL),
                normalizeScenario(scenario, ModelCallScenario.CHAT_STREAM),
                promptSummary,
                responseSummary,
                response.promptTokens(),
                response.completionTokens(),
                latencyMs,
                ModelCallStatus.SUCCESS,
                null,
                null,
                traceId,
                null,
                null,
                null,
                true);
    }

    @Transactional
    public void recordFailure(
            UUID conversationId,
            UUID messageId,
            String provider,
            String model,
            ModelCallScenario scenario,
            long latencyMs,
            Throwable failure,
            String traceId) {
        recordFailure(
                conversationId,
                messageId,
                provider,
                model,
                normalizeScenario(scenario, ModelCallScenario.CHAT_SYNC),
                latencyMs,
                failure,
                traceId,
                false);
    }

    @Transactional
    public void recordStreamFailure(
            UUID conversationId,
            UUID messageId,
            String provider,
            String model,
            ModelCallScenario scenario,
            long latencyMs,
            Throwable failure,
            String traceId) {
        recordFailure(
                conversationId,
                messageId,
                provider,
                model,
                normalizeScenario(scenario, ModelCallScenario.CHAT_STREAM),
                latencyMs,
                failure,
                traceId,
                true);
    }

    @Transactional
    public void recordStreamTimeout(
            UUID conversationId,
            UUID messageId,
            String provider,
            String model,
            ModelCallScenario scenario,
            long latencyMs,
            Throwable failure,
            String traceId) {
        recordCall(
                conversationId,
                messageId,
                normalize(provider, DEFAULT_PROVIDER),
                normalize(model, DEFAULT_MODEL),
                normalizeScenario(scenario, ModelCallScenario.CHAT_STREAM),
                null,
                null,
                null,
                null,
                latencyMs,
                ModelCallStatus.TIMEOUT,
                MODEL_CALL_TIMEOUT,
                sanitize(failure),
                traceId,
                null,
                null,
                null,
                true);
    }

    @Transactional
    public void recordStreamCancelled(
            UUID conversationId,
            UUID messageId,
            String provider,
            String model,
            ModelCallScenario scenario,
            long latencyMs,
            String reason,
            String traceId) {
        recordCall(
                conversationId,
                messageId,
                normalize(provider, DEFAULT_PROVIDER),
                normalize(model, DEFAULT_MODEL),
                normalizeScenario(scenario, ModelCallScenario.CHAT_STREAM),
                null,
                null,
                null,
                null,
                latencyMs,
                ModelCallStatus.CANCELLED,
                MODEL_CALL_CANCELLED,
                auditSummarySanitizer.sanitizeText(reason, ERROR_MESSAGE_MAX_LENGTH),
                traceId,
                null,
                null,
                null,
                true);
    }

    private void recordFailure(
            UUID conversationId,
            UUID messageId,
            String provider,
            String model,
            ModelCallScenario scenario,
            long latencyMs,
            Throwable failure,
            String traceId,
            boolean streaming) {
        recordCall(
                conversationId,
                messageId,
                normalize(provider, DEFAULT_PROVIDER),
                normalize(model, DEFAULT_MODEL),
                scenario,
                null,
                null,
                null,
                null,
                latencyMs,
                ModelCallStatus.FAILED,
                MODEL_CALL_FAILED,
                sanitize(failure),
                traceId,
                null,
                null,
                null,
                streaming);
    }

    private String normalize(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    private ModelCallScenario normalizeScenario(ModelCallScenario scenario, ModelCallScenario fallback) {
        return scenario == null ? fallback : scenario;
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
            String promptSummary,
            String responseSummary,
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
        recordCall(
                conversationId,
                messageId,
                normalize(provider, DEFAULT_PROVIDER),
                normalize(model, DEFAULT_MODEL),
                scenarioFromAgentStage(agentStage),
                promptSummary,
                responseSummary,
                promptTokens,
                completionTokens,
                latencyMs,
                status,
                errorCode,
                auditSummarySanitizer.sanitizeText(errorMessage, ERROR_MESSAGE_MAX_LENGTH),
                traceId,
                agentStage,
                agentRunId,
                agentStepId,
                false);
    }

    @Transactional
    public void recordGraphRagExtraction(
            String provider,
            String model,
            String promptSummary,
            String responseSummary,
            long latencyMs,
            ModelCallStatus status,
            String errorCode,
            String errorMessage,
            String traceId) {
        recordCall(
                null,
                null,
                normalize(provider, DEFAULT_PROVIDER),
                normalize(model, DEFAULT_MODEL),
                ModelCallScenario.GRAPHRAG_EXTRACTION,
                promptSummary,
                responseSummary,
                null,
                null,
                latencyMs,
                status,
                errorCode,
                auditSummarySanitizer.sanitizeText(errorMessage, ERROR_MESSAGE_MAX_LENGTH),
                traceId,
                "GRAPHRAG_EXTRACTION",
                null,
                null,
                false);
    }

    private void recordCall(
            UUID conversationId,
            UUID messageId,
            String provider,
            String model,
            ModelCallScenario scenario,
            String promptSummary,
            String responseSummary,
            Integer promptTokens,
            Integer completionTokens,
            long latencyMs,
            ModelCallStatus status,
            String errorCode,
            String errorMessage,
            String traceId,
            String agentStage,
            UUID agentRunId,
            UUID agentStepId,
            boolean streaming) {
        ModelCallLogEntity entity = save(
                conversationId,
                messageId,
                provider,
                model,
                scenario,
                promptSummary,
                responseSummary,
                promptTokens,
                completionTokens,
                latencyMs,
                status,
                errorCode,
                errorMessage,
                traceId,
                agentStage,
                agentRunId,
                agentStepId);
        recordAudit(
                entity,
                status == ModelCallStatus.SUCCESS ? AuditResult.SUCCESS : AuditResult.FAILURE,
                entity.getErrorMessage());
        agentWeaveMetrics.recordModelCall(entity, streaming);
    }

    private ModelCallLogEntity save(
            UUID conversationId,
            UUID messageId,
            String provider,
            String model,
            ModelCallScenario scenario,
            String promptSummary,
            String responseSummary,
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
        String normalizedTraceId = normalize(traceId, "unknown");
        return modelCallLogRepository.save(new ModelCallLogEntity(
                UUID.randomUUID(),
                conversationId,
                messageId,
                provider,
                model,
                scenario,
                auditSummarySanitizer.sanitizeText(promptSummary, SUMMARY_MAX_LENGTH),
                auditSummarySanitizer.sanitizeText(responseSummary, SUMMARY_MAX_LENGTH),
                promptTokens,
                completionTokens,
                latencyMs,
                status,
                errorCode,
                errorMessage,
                normalizedTraceId,
                agentStage,
                agentRunId,
                agentStepId));
    }

    private ModelCallScenario scenarioFromAgentStage(String agentStage) {
        if (agentStage == null || agentStage.isBlank()) {
            return ModelCallScenario.EXECUTOR;
        }
        return switch (agentStage.trim().toUpperCase()) {
            case "PLANNER", "PLANNING" -> ModelCallScenario.PLANNER;
            case "REVIEWER", "REVIEW" -> ModelCallScenario.REVIEWER;
            case "GRAPHRAG_EXTRACTION" -> ModelCallScenario.GRAPHRAG_EXTRACTION;
            case "RAG_ANSWER" -> ModelCallScenario.RAG_ANSWER;
            default -> ModelCallScenario.EXECUTOR;
        };
    }

    private void recordAudit(ModelCallLogEntity entity, AuditResult result, String errorMessage) {
        auditLogService.record(new AuditLogCommand(
                AuditEventType.MODEL_CALL,
                null,
                null,
                "model_call",
                entity.getId().toString(),
                "CALL_MODEL",
                result,
                entity.getLatencyMs(),
                "provider=" + entity.getProvider()
                        + ";model=" + entity.getModel()
                        + ";conversationId=" + entity.getConversationId()
                        + ";messageId=" + entity.getMessageId()
                        + ";promptTokens=" + entity.getPromptTokens()
                        + ";completionTokens=" + entity.getCompletionTokens(),
                null,
                errorMessage));
    }
}
