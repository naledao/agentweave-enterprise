package com.agentweave.graphrag.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "agentweave.graphrag.neo4j")
public record GraphRagNeo4jProperties(
        boolean enabled,
        String baseUrl,
        String database,
        String username,
        String password) {
}
