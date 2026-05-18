package com.agentweave.graphrag.domain;

import com.agentweave.conversation.domain.ConversationEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "graphrag_retrieval_logs")
public class GraphRagRetrievalLog {

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

    @Column(name = "business_domain", length = 120)
    private String businessDomain;

    @Column(name = "permission_level", length = 80)
    private String permissionLevel;

    @Column(name = "document_id")
    private UUID documentId;

    @Column(name = "max_depth", nullable = false)
    private int maxDepth;

    @Column(name = "max_path_count", nullable = false)
    private int maxPathCount;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "resolved_entities", nullable = false, columnDefinition = "jsonb")
    private List<String> resolvedEntities = new ArrayList<>();

    @Column(name = "matched_path_count", nullable = false)
    private int matchedPathCount;

    @Column(name = "filtered_path_count", nullable = false)
    private int filteredPathCount;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "source_chunk_ids", nullable = false, columnDefinition = "jsonb")
    private List<String> sourceChunkIds = new ArrayList<>();

    @Column(name = "confidence_summary", length = 1000)
    private String confidenceSummary;

    @Column(name = "duration_ms", nullable = false)
    private long durationMs;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private GraphRagRetrievalStatus status;

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

    protected GraphRagRetrievalLog() {
    }

    public GraphRagRetrievalLog(
            UUID id,
            UUID conversationId,
            UUID messageId,
            UUID workflowRunId,
            UUID workflowStepId,
            String traceId,
            String query,
            String retrievalMode,
            String businessDomain,
            String permissionLevel,
            UUID documentId,
            int maxDepth,
            int maxPathCount,
            List<String> resolvedEntities) {
        this.id = id;
        this.conversationId = conversationId;
        this.messageId = messageId;
        this.workflowRunId = workflowRunId;
        this.workflowStepId = workflowStepId;
        this.traceId = traceId;
        this.query = query;
        this.retrievalMode = retrievalMode;
        this.businessDomain = businessDomain;
        this.permissionLevel = permissionLevel;
        this.documentId = documentId;
        this.maxDepth = maxDepth;
        this.maxPathCount = maxPathCount;
        this.resolvedEntities = resolvedEntities == null ? new ArrayList<>() : new ArrayList<>(resolvedEntities);
        this.status = GraphRagRetrievalStatus.PROCESSING;
        this.matchedPathCount = 0;
        this.filteredPathCount = 0;
        this.durationMs = 0;
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

    public String getBusinessDomain() {
        return businessDomain;
    }

    public String getPermissionLevel() {
        return permissionLevel;
    }

    public UUID getDocumentId() {
        return documentId;
    }

    public int getMaxDepth() {
        return maxDepth;
    }

    public int getMaxPathCount() {
        return maxPathCount;
    }

    public List<String> getResolvedEntities() {
        return List.copyOf(resolvedEntities);
    }

    public int getMatchedPathCount() {
        return matchedPathCount;
    }

    public int getFilteredPathCount() {
        return filteredPathCount;
    }

    public List<String> getSourceChunkIds() {
        return List.copyOf(sourceChunkIds);
    }

    public String getConfidenceSummary() {
        return confidenceSummary;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public GraphRagRetrievalStatus getStatus() {
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

    public void markCompleted(
            int matchedPathCount,
            int filteredPathCount,
            List<String> resolvedEntities,
            List<String> sourceChunkIds,
            String confidenceSummary) {
        this.status = GraphRagRetrievalStatus.SUCCESS;
        this.matchedPathCount = matchedPathCount;
        this.filteredPathCount = filteredPathCount;
        this.resolvedEntities = resolvedEntities == null ? new ArrayList<>() : new ArrayList<>(resolvedEntities);
        this.sourceChunkIds = sourceChunkIds == null ? new ArrayList<>() : new ArrayList<>(sourceChunkIds);
        this.confidenceSummary = confidenceSummary;
        this.errorMessage = null;
        complete();
    }

    public void markFailed(String errorMessage) {
        this.status = GraphRagRetrievalStatus.FAILED;
        this.errorMessage = errorMessage;
        complete();
    }

    public void markDegraded(String errorMessage) {
        this.status = GraphRagRetrievalStatus.DEGRADED;
        this.errorMessage = errorMessage;
        complete();
    }

    private void complete() {
        this.completedAt = Instant.now();
        this.durationMs = Math.max(0, completedAt.toEpochMilli() - startedAt.toEpochMilli());
    }
}
