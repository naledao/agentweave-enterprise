package com.agentweave.graphrag.application;

import com.agentweave.graphrag.domain.KnowledgeGraphEntityType;
import com.agentweave.graphrag.dto.GraphRagChunkExtraction;
import com.agentweave.graphrag.dto.GraphRagExtractionCommand;
import com.agentweave.graphrag.dto.GraphRagExtractionResult;
import com.agentweave.knowledge.domain.DocumentChunkEntity;
import com.agentweave.knowledge.domain.DocumentEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class GraphRagExtractionService {

    private final KnowledgeGraphExtractionAgent extractionAgent;

    public GraphRagExtractionService(KnowledgeGraphExtractionAgent extractionAgent) {
        this.extractionAgent = extractionAgent;
    }

    public List<GraphRagChunkExtraction> extract(DocumentEntity document, List<DocumentChunkEntity> chunks) {
        List<GraphRagChunkExtraction> extractions = new ArrayList<>();
        for (DocumentChunkEntity chunk : chunks) {
            GraphRagExtractionCommand command = new GraphRagExtractionCommand(
                    document.getId(),
                    chunk.getId(),
                    chunk.getContent(),
                    document.getBusinessDomain(),
                    document.getDocumentType(),
                    document.getPermissionLevel());
            GraphRagExtractionResult result = extractionAgent.extract(command);
            extractions.add(new GraphRagChunkExtraction(
                    chunk.getId(),
                    result.entities(),
                    result.relationships()));
        }
        return extractions;
    }
}
