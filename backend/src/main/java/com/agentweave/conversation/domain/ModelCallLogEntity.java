package com.agentweave.conversation.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "model_call_logs")
public class ModelCallLogEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
    @JoinColumn(name = "conversation_id", insertable = false, updatable = false)
    private ConversationEntity conversation;

    @Column(name = "conversation_id")
    private UUID conversationId;

    @Column(name = "message_id")
    private UUID messageId;

    @Column(nullable = false, length = 80)
    private String provider;

    @Column(length = 160)
    private String model;

    @Column(name = "model_name", nullable = false, length = 160)
    private String modelName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 60)
    private ModelCallScenario scenario;

    @Column(name = "prompt_summary", columnDefinition = "TEXT")
    private String promptSummary;

    @Column(name = "response_summary", columnDefinition = "TEXT")
    private String responseSummary;

    @Column(name = "prompt_tokens")
    private Integer promptTokens;

    @Column(name = "completion_tokens")
    private Integer completionTokens;

    @Column(name = "input_tokens")
    private Integer inputTokens;

    @Column(name = "output_tokens")
    private Integer outputTokens;

    @Column(name = "total_tokens")
    private Integer totalTokens;

    @Column(name = "latency_ms")
    private Long latencyMs;

    @Column(name = "duration_ms", nullable = false)
    private long durationMs;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ModelCallStatus status;

    @Column(name = "error_code", length = 120)
    private String errorCode;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "trace_id", nullable = false, length = 120)
    private String traceId;

    @Column(name = "agent_stage", length = 50)
    private String agentStage;

    @Column(name = "agent_run_id")
    private UUID agentRunId;

    @Column(name = "agent_step_id")
    private UUID agentStepId;

    @Column(name = "workflow_run_id")
    private UUID workflowRunId;

    @Column(name = "workflow_step_id")
    private UUID workflowStepId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ModelCallLogEntity() {
    }

    public ModelCallLogEntity(
            UUID id,
            UUID conversationId,
            UUID messageId,
            String provider,
            String modelName,
            ModelCallScenario scenario,
            String promptSummary,
            String responseSummary,
            Integer promptTokens,
            Integer completionTokens,
            long durationMs,
            ModelCallStatus status,
            String errorCode,
            String errorMessage,
            String traceId) {
        this(
                id,
                conversationId,
                messageId,
                provider,
                modelName,
                scenario,
                promptSummary,
                responseSummary,
                promptTokens,
                completionTokens,
                durationMs,
                status,
                errorCode,
                errorMessage,
                traceId,
                null,
                null,
                null);
    }

    public ModelCallLogEntity(
            UUID id,
            UUID conversationId,
            UUID messageId,
            String provider,
            String modelName,
            ModelCallScenario scenario,
            String promptSummary,
            String responseSummary,
            Integer promptTokens,
            Integer completionTokens,
            long durationMs,
            ModelCallStatus status,
            String errorCode,
            String errorMessage,
            String traceId,
            String agentStage,
            UUID agentRunId,
            UUID agentStepId) {
        this.id = id;
        this.conversationId = conversationId;
        this.messageId = messageId;
        this.provider = provider;
        this.model = modelName;
        this.modelName = modelName;
        this.scenario = scenario;
        this.promptSummary = promptSummary;
        this.responseSummary = responseSummary;
        this.promptTokens = promptTokens;
        this.completionTokens = completionTokens;
        this.inputTokens = promptTokens;
        this.outputTokens = completionTokens;
        this.totalTokens = totalTokens(promptTokens, completionTokens);
        this.latencyMs = durationMs;
        this.durationMs = durationMs;
        this.status = status;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.traceId = traceId;
        this.agentStage = agentStage;
        this.agentRunId = agentRunId;
        this.agentStepId = agentStepId;
        this.workflowRunId = agentRunId;
        this.workflowStepId = agentStepId;
    }

    public UUID getId() {
        return id;
    }

    public UUID getConversationId() {
        return conversationId;
    }

    public UUID getMessageId() {
        return messageId;
    }

    public String getProvider() {
        return provider;
    }

    public String getModel() {
        return modelName;
    }

    public String getModelName() {
        return modelName;
    }

    public ModelCallScenario getScenario() {
        return scenario;
    }

    public String getPromptSummary() {
        return promptSummary;
    }

    public String getResponseSummary() {
        return responseSummary;
    }

    public Integer getPromptTokens() {
        return inputTokens;
    }

    public Integer getCompletionTokens() {
        return outputTokens;
    }

    public Integer getInputTokens() {
        return inputTokens;
    }

    public Integer getOutputTokens() {
        return outputTokens;
    }

    public Integer getTotalTokens() {
        return totalTokens;
    }

    public long getLatencyMs() {
        return durationMs;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public ModelCallStatus getStatus() {
        return status;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getTraceId() {
        return traceId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getAgentStage() {
        return agentStage;
    }

    public UUID getAgentRunId() {
        return agentRunId;
    }

    public UUID getAgentStepId() {
        return agentStepId;
    }

    public UUID getWorkflowRunId() {
        return workflowRunId;
    }

    public UUID getWorkflowStepId() {
        return workflowStepId;
    }

    public ConversationEntity getConversation() {
        return conversation;
    }

    private Integer totalTokens(Integer inputTokens, Integer outputTokens) {
        if (inputTokens == null && outputTokens == null) {
            return null;
        }
        return valueOrZero(inputTokens) + valueOrZero(outputTokens);
    }

    private int valueOrZero(Integer value) {
        return value == null ? 0 : value;
    }
}
