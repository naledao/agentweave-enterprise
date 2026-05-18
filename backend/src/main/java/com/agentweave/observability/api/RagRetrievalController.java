package com.agentweave.observability.api;

import com.agentweave.observability.application.RagRetrievalQueryService;
import com.agentweave.observability.dto.RagRetrievalListResponse;
import com.agentweave.observability.dto.RagRetrievalQueryRequest;
import com.agentweave.observability.dto.RagRetrievalResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/observability/rag-retrievals")
public class RagRetrievalController {

    private final RagRetrievalQueryService ragRetrievalQueryService;

    public RagRetrievalController(RagRetrievalQueryService ragRetrievalQueryService) {
        this.ragRetrievalQueryService = ragRetrievalQueryService;
    }

    @GetMapping
    public RagRetrievalListResponse list(@Valid RagRetrievalQueryRequest request) {
        return ragRetrievalQueryService.list(request);
    }

    @GetMapping("/{id}")
    public RagRetrievalResponse get(@PathVariable UUID id) {
        return ragRetrievalQueryService.get(id);
    }
}
