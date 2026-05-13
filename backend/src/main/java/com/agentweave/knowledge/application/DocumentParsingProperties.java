package com.agentweave.knowledge.application;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "agentweave.knowledge.parsing")
public record DocumentParsingProperties(
        @NotEmpty List<@NotBlank String> allowedContentTypes,
        @NotEmpty List<@NotBlank String> allowedExtensions) {
}
