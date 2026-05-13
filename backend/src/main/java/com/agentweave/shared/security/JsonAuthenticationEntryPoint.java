package com.agentweave.shared.security;

import com.agentweave.shared.exception.ApiErrorResponse;
import com.agentweave.shared.exception.ErrorCode;
import com.agentweave.shared.tracing.TraceIdProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;
    private final TraceIdProvider traceIdProvider;

    public JsonAuthenticationEntryPoint(ObjectMapper objectMapper, TraceIdProvider traceIdProvider) {
        this.objectMapper = objectMapper;
        this.traceIdProvider = traceIdProvider;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        String traceId = traceIdProvider.currentTraceId(request);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("X-Trace-Id", traceId);
        ApiErrorResponse body = new ApiErrorResponse(
                ErrorCode.UNAUTHORIZED.code(),
                ErrorCode.UNAUTHORIZED.defaultMessage(),
                request.getRequestURI(),
                traceId,
                Instant.now());
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
