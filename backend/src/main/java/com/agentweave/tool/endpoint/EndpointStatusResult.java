package com.agentweave.tool.endpoint;

import java.time.Instant;

public record EndpointStatusResult(
        String endpoint,
        int httpStatus,
        long averageLatencyMs,
        double failureRate,
        Instant checkedAt) {
}
