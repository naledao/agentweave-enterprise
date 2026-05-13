package com.agentweave.knowledge.dto;

import java.util.List;
import org.springframework.data.domain.Page;

public record DocumentListResponse(
        List<DocumentResponse> items,
        int page,
        int size,
        long total,
        int totalPages) {

    public static DocumentListResponse from(Page<DocumentResponse> documents) {
        return new DocumentListResponse(
                documents.getContent(),
                documents.getNumber(),
                documents.getSize(),
                documents.getTotalElements(),
                documents.getTotalPages());
    }
}
