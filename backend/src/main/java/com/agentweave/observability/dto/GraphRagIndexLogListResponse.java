package com.agentweave.observability.dto;

import java.util.List;
import org.springframework.data.domain.Page;

public record GraphRagIndexLogListResponse(
        List<GraphRagIndexLogResponse> items,
        int page,
        int size,
        long total,
        int totalPages) {

    public static GraphRagIndexLogListResponse from(Page<GraphRagIndexLogResponse> logs) {
        return new GraphRagIndexLogListResponse(
                logs.getContent(),
                logs.getNumber(),
                logs.getSize(),
                logs.getTotalElements(),
                logs.getTotalPages());
    }
}
