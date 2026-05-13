package com.agentweave.shared.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "agentweave.security.jwt")
public record JwtProperties(
        @NotBlank String secret,
        @NotBlank String issuer,
        @NotNull Duration accessTokenTtl) {

    public JwtProperties {
        if (accessTokenTtl != null && accessTokenTtl.compareTo(Duration.ofMinutes(5)) < 0) {
            throw new IllegalArgumentException("access-token-ttl must be at least 5 minutes");
        }
    }
}
