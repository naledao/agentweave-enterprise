package com.agentweave.shared.security;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "agentweave.bootstrap")
public record BootstrapAdminProperties(
        @NotBlank String adminUsername,
        @NotBlank String adminPassword,
        @NotBlank @Email String adminEmail) {
}
