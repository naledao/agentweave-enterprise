package com.agentweave.observability.dto;

import com.agentweave.tool.dto.ToolInvocationListResponse;
import java.util.List;

public record ToolInvocationSummaryResponse(
        long total,
        long running,
        long success,
        long failed,
        long denied,
        long timeout,
        double failureRate,
        double deniedRate,
        double timeoutRate,
        double averageDurationMs,
        List<ToolInvocationStatusCount> statusCounts,
        List<ToolInvocationToolCount> toolCounts,
        ToolInvocationListResponse invocations) {
}
