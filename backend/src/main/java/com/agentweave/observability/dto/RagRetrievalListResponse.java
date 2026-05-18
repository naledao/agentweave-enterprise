package com.agentweave.observability.dto;

import java.util.List;
import org.springframework.data.domain.Page;

public record RagRetrievalListResponse(
        List<RagRetrievalResponse> items,
        int page,
        int size,
        long total,
        int totalPages) {

    public static RagRetrievalListResponse from(Page<RagRetrievalResponse> retrievals) {
        return new RagRetrievalListResponse(
                retrievals.getContent(),
                retrievals.getNumber(),
                retrievals.getSize(),
                retrievals.getTotalElements(),
                retrievals.getTotalPages());
    }
}
