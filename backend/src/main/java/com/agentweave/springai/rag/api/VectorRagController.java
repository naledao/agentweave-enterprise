package com.agentweave.springai.rag.api;

import com.agentweave.springai.rag.VectorRetrievalService;
import com.agentweave.springai.rag.dto.VectorRagSearchRequest;
import com.agentweave.springai.rag.dto.VectorRagSearchResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/rag")
public class VectorRagController {

    private final VectorRetrievalService vectorRetrievalService;

    public VectorRagController(VectorRetrievalService vectorRetrievalService) {
        this.vectorRetrievalService = vectorRetrievalService;
    }

    @PostMapping("/search")
    @PreAuthorize("hasAuthority('knowledge:rag:search') or hasAuthority('ROLE_ADMIN')")
    public VectorRagSearchResponse search(@Valid @RequestBody VectorRagSearchRequest request) {
        return vectorRetrievalService.search(request);
    }
}
