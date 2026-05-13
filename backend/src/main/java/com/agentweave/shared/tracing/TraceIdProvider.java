package com.agentweave.shared.tracing;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Component
public class TraceIdProvider {

    public static final String TRACE_ID_KEY = "traceId";
    private static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final String TRACE_ID_HEADER = "X-Trace-Id";

    public String currentTraceId() {
        String existing = MDC.get(TRACE_ID_KEY);
        if (existing != null && !existing.isBlank()) {
            return existing;
        }
        String traceId = UUID.randomUUID().toString();
        MDC.put(TRACE_ID_KEY, traceId);
        return traceId;
    }

    public String currentTraceId(HttpServletRequest request) {
        String headerTraceId = Optional.ofNullable(request.getHeader(TRACE_ID_HEADER))
                .or(() -> Optional.ofNullable(request.getHeader(REQUEST_ID_HEADER)))
                .filter(value -> !value.isBlank())
                .orElse(null);
        if (headerTraceId != null) {
            MDC.put(TRACE_ID_KEY, headerTraceId);
            return headerTraceId;
        }
        String existing = MDC.get(TRACE_ID_KEY);
        if (existing != null && !existing.isBlank()) {
            return existing;
        }
        String traceId = UUID.randomUUID().toString();
        MDC.put(TRACE_ID_KEY, traceId);
        return traceId;
    }
}
