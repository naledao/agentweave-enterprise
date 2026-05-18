package com.agentweave.observability.dto;

import java.util.List;
import org.springframework.data.domain.Page;

public record GraphRagRetrievalLogListResponse(
        List<GraphRagRetrievalLogResponse> items,
        int page,
        int size,
        long total,
        int totalPages) {

    public static GraphRagRetrievalLogListResponse from(Page<GraphRagRetrievalLogResponse> logs) {
        return new GraphRagRetrievalLogListResponse(
                logs.getContent(),
                logs.getNumber(),
                logs.getSize(),
                logs.getTotalElements(),
                logs.getTotalPages());
    }
}
