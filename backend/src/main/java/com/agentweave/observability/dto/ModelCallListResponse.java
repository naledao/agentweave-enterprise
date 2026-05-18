package com.agentweave.observability.dto;

import java.util.List;
import org.springframework.data.domain.Page;

public record ModelCallListResponse(
        List<ModelCallResponse> items,
        int page,
        int size,
        long total,
        int totalPages) {

    public static ModelCallListResponse from(Page<ModelCallResponse> modelCalls) {
        return new ModelCallListResponse(
                modelCalls.getContent(),
                modelCalls.getNumber(),
                modelCalls.getSize(),
                modelCalls.getTotalElements(),
                modelCalls.getTotalPages());
    }
}
