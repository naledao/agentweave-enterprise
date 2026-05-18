package com.agentweave.observability.dto;

import com.agentweave.conversation.domain.ModelCallScenario;
import com.agentweave.conversation.domain.ModelCallStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import org.springframework.format.annotation.DateTimeFormat;

public record ModelCallQueryRequest(
        @Min(0)
        Integer page,

        @Min(1)
        @Max(100)
        Integer size,

        @Size(max = 160)
        String modelName,

        ModelCallScenario scenario,

        ModelCallStatus status,

        @Size(max = 120)
        String traceId,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        Instant createdFrom,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        Instant createdTo) {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    public int pageNumber() {
        return page == null ? DEFAULT_PAGE : page;
    }

    public int pageSize() {
        if (size == null) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }

    public String normalizedModelName() {
        return blankToNull(modelName);
    }

    public String normalizedTraceId() {
        return blankToNull(traceId);
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
