package com.agentweave.tool.log;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record LogQueryRequest(
        @NotBlank
        @Size(max = 80)
        String serviceName,

        @NotBlank
        @Size(max = 120)
        String keyword,

        @NotNull
        @Valid
        TimeRange timeRange,

        @Min(1)
        @Max(50)
        Integer limit) {

    public String normalizedServiceName() {
        return serviceName == null ? null : serviceName.trim().toLowerCase();
    }

    public String normalizedKeyword() {
        return keyword == null ? null : keyword.trim();
    }

    public int requestedLimit(int defaultLimit) {
        return limit == null ? defaultLimit : limit;
    }

    public record TimeRange(
            @NotNull
            Instant from,

            @NotNull
            Instant to) {
    }
}
