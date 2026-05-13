package com.agentweave.conversation.application;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "agentweave.chat")
public record ChatProperties(
        Duration streamTimeout,
        Integer maxConcurrentStreamsPerUser) {

    private static final Duration DEFAULT_STREAM_TIMEOUT = Duration.ofSeconds(120);
    private static final int DEFAULT_MAX_CONCURRENT_STREAMS_PER_USER = 3;

    public ChatProperties {
        if (streamTimeout == null) {
            streamTimeout = DEFAULT_STREAM_TIMEOUT;
        }
        if (streamTimeout.isNegative() || streamTimeout.isZero()) {
            throw new IllegalArgumentException("stream-timeout must be greater than zero");
        }
        if (maxConcurrentStreamsPerUser == null) {
            maxConcurrentStreamsPerUser = DEFAULT_MAX_CONCURRENT_STREAMS_PER_USER;
        }
        if (maxConcurrentStreamsPerUser < 1) {
            throw new IllegalArgumentException("max-concurrent-streams-per-user must be at least 1");
        }
    }
}
