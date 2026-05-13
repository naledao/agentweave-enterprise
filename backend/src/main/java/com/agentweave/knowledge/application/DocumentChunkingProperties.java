package com.agentweave.knowledge.application;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "agentweave.knowledge.chunking")
public record DocumentChunkingProperties(
        @DefaultValue("1000") @Min(100) @Max(20000) int chunkSize,
        @DefaultValue("200") @Min(0) @Max(5000) int overlapSize) {

    public DocumentChunkingProperties {
        if (overlapSize >= chunkSize) {
            throw new IllegalArgumentException("overlapSize must be less than chunkSize");
        }
    }
}
