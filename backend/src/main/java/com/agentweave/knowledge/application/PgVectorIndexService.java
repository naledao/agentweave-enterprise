package com.agentweave.knowledge.application;

import com.agentweave.knowledge.domain.DocumentChunkEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;

@Service
public class PgVectorIndexService {

    private final VectorStore vectorStore;

    public PgVectorIndexService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public void index(List<DocumentChunkEntity> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return;
        }
        vectorStore.add(chunks.stream()
                .map(this::toSpringAiDocument)
                .toList());
    }

    public void deleteByDocumentId(UUID documentId) {
        if (documentId == null) {
            return;
        }
        FilterExpressionBuilder builder = new FilterExpressionBuilder();
        vectorStore.delete(builder.eq("documentId", documentId.toString()).build());
    }

    private Document toSpringAiDocument(DocumentChunkEntity chunk) {
        return Document.builder()
                .id(chunk.getId().toString())
                .text(chunk.getContent())
                .metadata(chunk.getMetadata())
                .build();
    }
}
