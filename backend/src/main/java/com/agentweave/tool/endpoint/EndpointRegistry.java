package com.agentweave.tool.endpoint;

import com.agentweave.shared.exception.BusinessException;
import com.agentweave.shared.exception.ErrorCode;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class EndpointRegistry {

    private final Map<String, EndpointRegistration> registrations;

    public EndpointRegistry() {
        this.registrations = registeredEndpoints();
    }

    public EndpointRegistration requireRegistered(String endpoint) {
        return find(endpoint)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.VALIDATION_FAILED,
                        "endpoint is not registered"));
    }

    public Optional<EndpointRegistration> find(String endpoint) {
        if (endpoint == null || endpoint.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(registrations.get(normalize(endpoint)));
    }

    private Map<String, EndpointRegistration> registeredEndpoints() {
        Map<String, EndpointRegistration> result = new LinkedHashMap<>();
        register(result, new EndpointRegistration(
                "knowledge-service",
                Set.of("knowledge-service", "/api/v1/documents", "documents-api")));
        register(result, new EndpointRegistration(
                "chat-service",
                Set.of("chat-service", "/api/v1/conversations", "/api/v1/conversations/stream", "chat-api")));
        register(result, new EndpointRegistration(
                "tool-service",
                Set.of("tool-service", "/api/v1/tools", "endpoint.status", "api-status-query")));
        register(result, new EndpointRegistration(
                "status-monitor-down",
                Set.of("status-monitor-down")));
        return Map.copyOf(result);
    }

    private void register(Map<String, EndpointRegistration> result, EndpointRegistration registration) {
        result.put(normalize(registration.endpoint()), registration);
        registration.aliases().forEach(alias -> result.put(normalize(alias), registration));
    }

    private String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
