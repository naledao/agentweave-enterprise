package com.agentweave.observability.dto;

import com.agentweave.graphrag.domain.GraphRagIndexStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;

public record GraphRagIndexLogQueryRequest(
        @Min(0)
        Integer page,

        @Min(1)
        @Max(100)
        Integer size,

        UUID documentId,

        @Size(max = 120)
        String traceId,

        GraphRagIndexStatus status,

        Boolean neo4jEnabled,

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

    public String normalizedTraceId() {
        if (traceId == null || traceId.isBlank()) {
            return null;
        }
        return traceId.trim();
    }
}
