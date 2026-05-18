package com.agentweave.observability.application;

import com.agentweave.graphrag.infrastructure.GraphRagNeo4jProperties;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class Neo4jHealthIndicator implements HealthIndicator {

    private final GraphRagNeo4jProperties properties;
    private final RestClient restClient;

    public Neo4jHealthIndicator(GraphRagNeo4jProperties properties) {
        this.properties = properties;
        RestClient.Builder builder = RestClient.builder()
                .defaultHeaders(headers -> {
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    headers.setAccept(List.of(MediaType.APPLICATION_JSON));
                    if (hasText(properties.username())) {
                        String token = Base64.getEncoder().encodeToString(
                                (properties.username() + ":" + safePassword()).getBytes(StandardCharsets.UTF_8));
                        headers.set("Authorization", "Basic " + token);
                    }
                });
        if (hasText(properties.baseUrl())) {
            builder.baseUrl(properties.baseUrl());
        }
        this.restClient = builder.build();
    }

    @Override
    public Health health() {
        if (!properties.enabled()) {
            return Health.up()
                    .withDetail("enabled", false)
                    .withDetail("status", "disabled")
                    .build();
        }
        if (!hasText(properties.baseUrl()) || !hasText(properties.database())) {
            return Health.down()
                    .withDetail("enabled", true)
                    .withDetail("baseUrlConfigured", hasText(properties.baseUrl()))
                    .withDetail("databaseConfigured", hasText(properties.database()))
                    .build();
        }
        try {
            Neo4jCommitResponse response = restClient.post()
                    .uri("/db/{database}/tx/commit", properties.database())
                    .body(Map.of("statements", List.of(Map.of("statement", "RETURN 1 AS ok"))))
                    .retrieve()
                    .body(Neo4jCommitResponse.class);
            if (response == null || (response.errors() != null && !response.errors().isEmpty())) {
                return Health.down()
                        .withDetail("enabled", true)
                        .withDetail("baseUrl", properties.baseUrl())
                        .withDetail("database", properties.database())
                        .withDetail("errorCount", response == null || response.errors() == null
                                ? 1
                                : response.errors().size())
                        .build();
            }
            return Health.up()
                    .withDetail("enabled", true)
                    .withDetail("baseUrl", properties.baseUrl())
                    .withDetail("database", properties.database())
                    .build();
        } catch (Exception ex) {
            return Health.down(ex)
                    .withDetail("enabled", true)
                    .withDetail("baseUrl", properties.baseUrl())
                    .withDetail("database", properties.database())
                    .build();
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String safePassword() {
        return properties.password() == null ? "" : properties.password();
    }

    private record Neo4jCommitResponse(List<Neo4jError> errors) {
    }

    private record Neo4jError(String code, String message) {
    }
}
