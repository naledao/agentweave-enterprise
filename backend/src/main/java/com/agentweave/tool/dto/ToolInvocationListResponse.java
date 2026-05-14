package com.agentweave.tool.dto;

import java.util.List;
import org.springframework.data.domain.Page;

public record ToolInvocationListResponse(
        List<ToolInvocationResponse> items,
        int page,
        int size,
        long total,
        int totalPages) {

    public static ToolInvocationListResponse from(Page<ToolInvocationResponse> invocations) {
        return new ToolInvocationListResponse(
                invocations.getContent(),
                invocations.getNumber(),
                invocations.getSize(),
                invocations.getTotalElements(),
                invocations.getTotalPages());
    }
}
