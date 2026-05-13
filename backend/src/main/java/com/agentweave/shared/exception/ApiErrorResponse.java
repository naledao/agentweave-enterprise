package com.agentweave.shared.exception;

import java.time.Instant;

public record ApiErrorResponse(
        String code,
        String message,
        String path,
        String traceId,
        Instant timestamp) {
}
