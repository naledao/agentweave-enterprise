package com.agentweave.observability.api;

import com.agentweave.observability.application.GraphRagObservabilityQueryService;
import com.agentweave.observability.dto.GraphRagIndexLogListResponse;
import com.agentweave.observability.dto.GraphRagIndexLogQueryRequest;
import com.agentweave.observability.dto.GraphRagRetrievalLogListResponse;
import com.agentweave.observability.dto.GraphRagRetrievalLogQueryRequest;
import com.agentweave.observability.dto.GraphRagSummaryResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/observability/graphrag")
public class GraphRagObservabilityController {

    private final GraphRagObservabilityQueryService graphRagObservabilityQueryService;

    public GraphRagObservabilityController(GraphRagObservabilityQueryService graphRagObservabilityQueryService) {
        this.graphRagObservabilityQueryService = graphRagObservabilityQueryService;
    }

    @GetMapping
    public GraphRagSummaryResponse summary() {
        return graphRagObservabilityQueryService.summary();
    }

    @GetMapping("/index-logs")
    public GraphRagIndexLogListResponse listIndexLogs(@Valid GraphRagIndexLogQueryRequest request) {
        return graphRagObservabilityQueryService.listIndexLogs(request);
    }

    @GetMapping("/retrieval-logs")
    public GraphRagRetrievalLogListResponse listRetrievalLogs(@Valid GraphRagRetrievalLogQueryRequest request) {
        return graphRagObservabilityQueryService.listRetrievalLogs(request);
    }
}
