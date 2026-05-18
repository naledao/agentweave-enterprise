package com.agentweave.springai.rag.domain;

import com.agentweave.conversation.domain.ConversationEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "rag_retrieval_logs")
public class RagRetrievalLog {

    @Id
    private UUID id;

    @Column(name = "trace_id", nullable = false, length = 120)
    private String traceId;

    @Column(name = "conversation_id")
    private UUID conversationId;

    @ManyToOne
    @JoinColumn(name = "conversation_id", insertable = false, updatable = false)
    private ConversationEntity conversation;

    @Column(name = "message_id")
    private UUID messageId;

    @Column(name = "workflow_run_id")
    private UUID workflowRunId;

    @Column(name = "workflow_step_id")
    private UUID workflowStepId;

    @Column(name = "query", nullable = false, columnDefinition = "TEXT")
    private String query;

    @Column(name = "retrieval_mode", nullable = false, length = 40)
    private String retrievalMode;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata_filter", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> metadataFilter = new LinkedHashMap<>();

    @Column(name = "business_domain", length = 120)
    private String businessDomain;

    @Column(name = "document_type", length = 120)
    private String documentType;

    @Column(name = "permission_level", length = 80)
    private String permissionLevel;

    @Column(name = "time_range", length = 160)
    private String timeRange;

    @Column(name = "document_id")
    private UUID documentId;

    @Column(name = "top_k", nullable = false)
    private int topK;

    @Column(name = "similarity_threshold", nullable = false)
    private double similarityThreshold;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "matched_chunk_ids", nullable = false, columnDefinition = "jsonb")
    private List<String> matchedChunkIds = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "citation_summaries", nullable = false, columnDefinition = "jsonb")
    private List<Map<String, Object>> citationSummaries = new ArrayList<>();

    @Column(name = "score_summary", length = 1000)
    private String scoreSummary;

    @Column(name = "citation_count", nullable = false)
    private int citationCount;

    @Column(name = "duration_ms", nullable = false)
    private long durationMs;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private RagRetrievalStatus status;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    protected RagRetrievalLog() {
    }

    public RagRetrievalLog(
            UUID id,
            UUID conversationId,
            UUID messageId,
            UUID workflowRunId,
            UUID workflowStepId,
            String traceId,
            String query,
            String retrievalMode,
            Map<String, Object> metadataFilter,
            String businessDomain,
            String documentType,
            String permissionLevel,
            String timeRange,
            UUID documentId,
            int topK,
            double similarityThreshold) {
        this.id = id;
        this.conversationId = conversationId;
        this.messageId = messageId;
        this.workflowRunId = workflowRunId;
        this.workflowStepId = workflowStepId;
        this.traceId = traceId;
        this.query = query;
        this.retrievalMode = retrievalMode;
        this.metadataFilter = metadataFilter == null ? new LinkedHashMap<>() : new LinkedHashMap<>(metadataFilter);
        this.businessDomain = businessDomain;
        this.documentType = documentType;
        this.permissionLevel = permissionLevel;
        this.timeRange = timeRange;
        this.documentId = documentId;
        this.topK = topK;
        this.similarityThreshold = similarityThreshold;
        this.status = RagRetrievalStatus.PROCESSING;
        this.startedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getTraceId() {
        return traceId;
    }

    public UUID getConversationId() {
        return conversationId;
    }

    public UUID getMessageId() {
        return messageId;
    }

    public UUID getWorkflowRunId() {
        return workflowRunId;
    }

    public UUID getWorkflowStepId() {
        return workflowStepId;
    }

    public String getQuery() {
        return query;
    }

    public String getRetrievalMode() {
        return retrievalMode;
    }

    public Map<String, Object> getMetadataFilter() {
        return Map.copyOf(metadataFilter);
    }

    public String getBusinessDomain() {
        return businessDomain;
    }

    public String getDocumentType() {
        return documentType;
    }

    public String getPermissionLevel() {
        return permissionLevel;
    }

    public String getTimeRange() {
        return timeRange;
    }

    public UUID getDocumentId() {
        return documentId;
    }

    public int getTopK() {
        return topK;
    }

    public double getSimilarityThreshold() {
        return similarityThreshold;
    }

    public List<String> getMatchedChunkIds() {
        return List.copyOf(matchedChunkIds);
    }

    public List<Map<String, Object>> getCitationSummaries() {
        return citationSummaries.stream()
                .map(Map::copyOf)
                .toList();
    }

    public String getScoreSummary() {
        return scoreSummary;
    }

    public int getCitationCount() {
        return citationCount;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public RagRetrievalStatus getStatus() {
        return status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void markSuccess(
            List<String> matchedChunkIds,
            List<Map<String, Object>> citationSummaries,
            String scoreSummary,
            int citationCount) {
        this.status = RagRetrievalStatus.SUCCESS;
        this.matchedChunkIds = matchedChunkIds == null ? new ArrayList<>() : new ArrayList<>(matchedChunkIds);
        this.citationSummaries = citationSummaries == null ? new ArrayList<>() : new ArrayList<>(citationSummaries);
        this.scoreSummary = scoreSummary;
        this.citationCount = Math.max(0, citationCount);
        this.errorMessage = null;
        completeNow();
    }

    public void markFailed(String errorMessage) {
        this.status = RagRetrievalStatus.FAILED;
        this.errorMessage = errorMessage;
        completeNow();
    }

    public void markDegraded(String errorMessage) {
        this.status = RagRetrievalStatus.DEGRADED;
        this.errorMessage = errorMessage;
        completeNow();
    }

    private void completeNow() {
        this.completedAt = Instant.now();
        this.durationMs = Duration.between(startedAt, completedAt).toMillis();
        if (this.durationMs < 0) {
            this.durationMs = 0;
        }
    }
}
