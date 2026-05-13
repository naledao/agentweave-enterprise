package com.agentweave.knowledge.application;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DataSizeUnit;
import org.springframework.util.unit.DataSize;
import org.springframework.util.unit.DataUnit;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "agentweave.knowledge.storage")
public record KnowledgeStorageProperties(
        @NotBlank String endpoint,
        @NotBlank String accessKey,
        @NotBlank String secretKey,
        @NotBlank String bucket,
        boolean createBucketIfMissing,
        @NotNull @DataSizeUnit(DataUnit.MEGABYTES) DataSize maxFileSize,
        @NotEmpty List<@NotBlank String> allowedContentTypes,
        @NotEmpty List<@NotBlank String> allowedExtensions) {

    public KnowledgeStorageProperties {
        if (maxFileSize != null && maxFileSize.toBytes() <= 0) {
            throw new IllegalArgumentException("maxFileSize must be positive");
        }
    }
}
