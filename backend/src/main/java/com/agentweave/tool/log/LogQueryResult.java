package com.agentweave.tool.log;

import java.time.Instant;
import java.util.List;

public record LogQueryResult(
        String summary,
        long hitCount,
        List<RecentLogError> recentErrors,
        TimeRangeResult timeRange) {

    public record RecentLogError(
            Instant timestamp,
            String serviceName,
            String level,
            String message,
            String traceId) {
    }

    public record TimeRangeResult(
            Instant from,
            Instant to) {
    }
}
