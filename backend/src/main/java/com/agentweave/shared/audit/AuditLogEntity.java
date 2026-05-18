package com.agentweave.shared.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "audit_logs")
public class AuditLogEntity {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 80)
    private AuditEventType eventType;

    private UUID userId;

    @Column(length = 80)
    private String username;

    @Column(length = 80)
    private String targetType;

    @Column(length = 120)
    private String targetId;

    @Column(length = 80)
    private String resourceType;

    @Column(length = 120)
    private String resourceId;

    @Column(nullable = false, length = 120)
    private String action;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AuditResult result;

    @Column(length = 500)
    private String reason;

    private Long durationMs;

    @Column(length = 1000)
    private String requestSummary;

    @Column(length = 1000)
    private String responseSummary;

    @Column(length = 500)
    private String errorMessage;

    @Column(length = 64)
    private String ipAddress;

    @Column(length = 500)
    private String userAgent;

    @Column(length = 120)
    private String traceId;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected AuditLogEntity() {
    }

    public AuditLogEntity(
            UUID id,
            AuditEventType eventType,
            UUID userId,
            String username,
            String targetType,
            String targetId,
            String action,
            AuditResult result,
            String reason,
            String ipAddress,
            String userAgent,
            String traceId) {
        this.id = id;
        this.eventType = eventType;
        this.userId = userId;
        this.username = username;
        this.targetType = targetType;
        this.targetId = targetId;
        this.resourceType = targetType;
        this.resourceId = targetId;
        this.action = action;
        this.result = result;
        this.reason = reason;
        this.errorMessage = reason;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.traceId = traceId;
    }

    public AuditLogEntity(
            UUID id,
            AuditEventType eventType,
            UUID userId,
            String username,
            String resourceType,
            String resourceId,
            String action,
            AuditResult result,
            Long durationMs,
            String requestSummary,
            String responseSummary,
            String errorMessage,
            String ipAddress,
            String userAgent,
            String traceId) {
        this.id = id;
        this.eventType = eventType;
        this.userId = userId;
        this.username = username;
        this.targetType = resourceType;
        this.targetId = resourceId;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.action = action;
        this.result = result;
        this.reason = errorMessage;
        this.durationMs = durationMs;
        this.requestSummary = requestSummary;
        this.responseSummary = responseSummary;
        this.errorMessage = errorMessage;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.traceId = traceId;
    }

    public UUID getId() {
        return id;
    }

    public AuditEventType getEventType() {
        return eventType;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getTargetType() {
        return targetType;
    }

    public String getTargetId() {
        return targetId;
    }

    public String getResourceType() {
        return resourceType == null ? targetType : resourceType;
    }

    public String getResourceId() {
        return resourceId == null ? targetId : resourceId;
    }

    public String getAction() {
        return action;
    }

    public AuditResult getResult() {
        return result;
    }

    public String getReason() {
        return reason;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public String getRequestSummary() {
        return requestSummary;
    }

    public String getResponseSummary() {
        return responseSummary;
    }

    public String getErrorMessage() {
        return errorMessage == null ? reason : errorMessage;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public String getTraceId() {
        return traceId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
