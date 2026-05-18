package com.agentweave.observability.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ObservabilitySummaryResponse(
        ModelCallSummary modelCallSummary,
        RagSummary ragSummary,
        GraphRagSummaryResponse graphRagSummary,
        ToolInvocationSummaryResponse toolSummary,
        WorkflowSummary workflowSummary,
        SseSummary sseSummary,
        HealthSummary healthSummary) {

    public record ModelCallSummary(
            long total,
            long failed,
            long timedOut,
            double failureRate,
            double timeoutRate,
            double averageDurationMs,
            Instant latestCreatedAt) {
    }

    public record RagSummary(
            long total,
            long successful,
            long failed,
            long degraded,
            double failureRate,
            double averageDurationMs,
            long citationCount,
            Instant latestCreatedAt) {
    }

    public record WorkflowSummary(
            long total,
            long running,
            long succeeded,
            long failed,
            long cancelled,
            double failureRate,
            double averageDurationMs,
            Instant latestCreatedAt) {
    }

    public record SseSummary(
            double activeConnections,
            double completedConnections,
            double failedConnections,
            double timedOutConnections,
            double averageConnectionDurationMs,
            double averageFirstTokenDurationMs) {
    }

    public record HealthSummary(
            String status,
            Map<String, String> components,
            List<String> groups) {
    }
}
