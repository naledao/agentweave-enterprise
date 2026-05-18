package com.agentweave.shared.tracing;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class TraceIdFilter extends OncePerRequestFilter {

    private final TraceIdProvider traceIdProvider;

    public TraceIdFilter(TraceIdProvider traceIdProvider) {
        this.traceIdProvider = traceIdProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String traceId = traceIdProvider.currentTraceId(request);
            response.setHeader("X-Trace-Id", traceId);
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(TraceIdProvider.TRACE_ID_KEY);
            MDC.remove(CorrelationContext.CONVERSATION_ID_KEY);
            MDC.remove(CorrelationContext.MESSAGE_ID_KEY);
            MDC.remove(CorrelationContext.WORKFLOW_RUN_ID_KEY);
            MDC.remove(CorrelationContext.WORKFLOW_STEP_ID_KEY);
        }
    }
}
