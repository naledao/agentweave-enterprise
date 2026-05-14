package com.agentweave.tool.log;

import java.time.Instant;

record LogRecord(
        Instant timestamp,
        String serviceName,
        String level,
        String message,
        String traceId) {
}
