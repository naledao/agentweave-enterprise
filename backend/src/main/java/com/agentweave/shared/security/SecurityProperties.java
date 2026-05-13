package com.agentweave.shared.security;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "agentweave.security")
public record SecurityProperties(
        boolean exposeOpenapi,
        @Valid Cors cors) {

    public record Cors(@NotEmpty List<String> allowedOrigins) {
    }
}
