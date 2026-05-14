package com.agentweave.tool.endpoint;

import java.time.Instant;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class EndpointStatusClient {

    private final Map<String, EndpointStatusResult> demoStatuses = Map.of(
            "knowledge-service",
            new EndpointStatusResult(
                    "knowledge-service",
                    200,
                    86,
                    0.012,
                    Instant.parse("2026-05-13T09:50:00Z")),
            "chat-service",
            new EndpointStatusResult(
                    "chat-service",
                    200,
                    124,
                    0.025,
                    Instant.parse("2026-05-13T15:55:00Z")),
            "tool-service",
            new EndpointStatusResult(
                    "tool-service",
                    200,
                    42,
                    0.004,
                    Instant.parse("2026-05-13T16:05:00Z")));

    public EndpointStatusResult query(EndpointRegistration registration) {
        if ("status-monitor-down".equals(registration.endpoint())) {
            throw new EndpointStatusException("endpoint status service unavailable");
        }
        EndpointStatusResult result = demoStatuses.get(registration.endpoint());
        if (result == null) {
            throw new EndpointStatusException("endpoint status is unavailable");
        }
        return result;
    }
}
