package com.agentweave.graphrag.application;

import com.agentweave.graphrag.dto.GraphRagIndexSummaryResponse;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class GraphRagIndexingJob {

    private final GraphRagIndexService graphRagIndexService;

    public GraphRagIndexingJob(GraphRagIndexService graphRagIndexService) {
        this.graphRagIndexService = graphRagIndexService;
    }

    public GraphRagIndexSummaryResponse execute(UUID documentId, String traceId) {
        return graphRagIndexService.index(documentId, traceId);
    }
}
