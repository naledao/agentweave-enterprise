package com.agentweave.tool.dto;

import com.agentweave.tool.domain.ToolInvocationStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import org.springframework.format.annotation.DateTimeFormat;

public record ToolInvocationQueryRequest(
        @Min(0)
        Integer page,

        @Min(1)
        @Max(100)
        Integer size,

        @Size(max = 120)
        String toolCode,

        @Size(max = 40)
        String status,

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

    public String normalizedToolCode() {
        if (toolCode == null || toolCode.isBlank()) {
            return null;
        }
        return toolCode.trim();
    }

    public ToolInvocationStatus normalizedStatus() {
        return ToolInvocationStatus.fromQuery(status);
    }
}
