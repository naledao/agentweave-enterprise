package com.agentweave.graphrag.application;

import com.agentweave.conversation.application.ModelCallLogService;
import com.agentweave.conversation.domain.ModelCallStatus;
import com.agentweave.graphrag.dto.GraphRagChunkExtraction;
import com.agentweave.graphrag.dto.GraphRagExtractionCommand;
import com.agentweave.graphrag.dto.GraphRagExtractionResult;
import com.agentweave.knowledge.domain.DocumentChunkEntity;
import com.agentweave.knowledge.domain.DocumentEntity;
import com.agentweave.shared.tracing.TraceIdProvider;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GraphRagExtractionService {

    private final KnowledgeGraphExtractionAgent extractionAgent;
    private final ModelCallLogService modelCallLogService;
    private final TraceIdProvider traceIdProvider;
    private final String modelName;

    public GraphRagExtractionService(
            KnowledgeGraphExtractionAgent extractionAgent,
            ModelCallLogService modelCallLogService,
            TraceIdProvider traceIdProvider,
            @Value("${spring.ai.openai.chat.options.model:unknown}") String modelName) {
        this.extractionAgent = extractionAgent;
        this.modelCallLogService = modelCallLogService;
        this.traceIdProvider = traceIdProvider;
        this.modelName = modelName;
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
            GraphRagExtractionResult result = extractWithObservability(command);
            extractions.add(new GraphRagChunkExtraction(
                    chunk.getId(),
                    result.entities(),
                    result.relationships()));
        }
        return extractions;
    }

    private GraphRagExtractionResult extractWithObservability(GraphRagExtractionCommand command) {
        long startedAt = System.nanoTime();
        String traceId = currentTraceId();
        try {
            GraphRagExtractionResult result = extractionAgent.extract(command);
            modelCallLogService.recordGraphRagExtraction(
                    "openai",
                    modelName,
                    promptSummary(command),
                    responseSummary(result),
                    elapsedMillis(startedAt),
                    ModelCallStatus.SUCCESS,
                    null,
                    null,
                    traceId);
            return result;
        } catch (RuntimeException ex) {
            modelCallLogService.recordGraphRagExtraction(
                    "openai",
                    modelName,
                    promptSummary(command),
                    null,
                    elapsedMillis(startedAt),
                    ModelCallStatus.FAILED,
                    "GRAPHRAG_EXTRACTION_FAILED",
                    ex.getMessage(),
                    traceId);
            throw ex;
        }
    }

    private String currentTraceId() {
        String traceId = MDC.get(TraceIdProvider.TRACE_ID_KEY);
        if (traceId != null && !traceId.isBlank()) {
            return traceId;
        }
        return traceIdProvider.currentTraceId();
    }

    private String promptSummary(GraphRagExtractionCommand command) {
        return "documentId=" + command.documentId()
                + ";chunkId=" + command.chunkId()
                + ";businessDomain=" + command.businessDomain()
                + ";documentType=" + command.documentType()
                + ";permissionLevel=" + command.permissionLevel()
                + ";chunkLength=" + safeLength(command.chunkContent());
    }

    private String responseSummary(GraphRagExtractionResult result) {
        return "entities=" + result.entities().size()
                + ";relationships=" + result.relationships().size();
    }

    private long elapsedMillis(long startedAt) {
        return Duration.ofNanos(System.nanoTime() - startedAt).toMillis();
    }

    private int safeLength(String value) {
        return value == null ? 0 : value.length();
    }
}
