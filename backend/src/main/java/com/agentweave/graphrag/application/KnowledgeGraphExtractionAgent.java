package com.agentweave.graphrag.application;

import com.agentweave.graphrag.dto.GraphRagExtractionCommand;
import com.agentweave.graphrag.dto.GraphRagExtractionResult;

public interface KnowledgeGraphExtractionAgent {

    GraphRagExtractionResult extract(GraphRagExtractionCommand command);
}
