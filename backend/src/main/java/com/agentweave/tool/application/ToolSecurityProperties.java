package com.agentweave.tool.application;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "agentweave.tool.security")
public record ToolSecurityProperties(
        Integer maxInvocationsPerMinute,
        Duration executionTimeout) {

    private static final int DEFAULT_MAX_INVOCATIONS_PER_MINUTE = 60;
    private static final Duration DEFAULT_EXECUTION_TIMEOUT = Duration.ofSeconds(5);

    public ToolSecurityProperties {
        if (maxInvocationsPerMinute == null) {
            maxInvocationsPerMinute = DEFAULT_MAX_INVOCATIONS_PER_MINUTE;
        }
        if (maxInvocationsPerMinute < 1) {
            throw new IllegalArgumentException("max-invocations-per-minute must be at least 1");
        }
        if (executionTimeout == null) {
            executionTimeout = DEFAULT_EXECUTION_TIMEOUT;
        }
        if (executionTimeout.isNegative() || executionTimeout.isZero()) {
            throw new IllegalArgumentException("execution-timeout must be greater than zero");
        }
    }
}
