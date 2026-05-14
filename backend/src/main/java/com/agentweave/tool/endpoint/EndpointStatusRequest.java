package com.agentweave.tool.endpoint;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record EndpointStatusRequest(
        @NotBlank
        @Size(max = 160)
        @Pattern(
                regexp = "^[A-Za-z0-9._:/-]+$",
                message = "must contain only endpoint identifier characters")
        String endpoint) {

    public String normalizedEndpoint() {
        return endpoint == null ? null : endpoint.trim();
    }
}
