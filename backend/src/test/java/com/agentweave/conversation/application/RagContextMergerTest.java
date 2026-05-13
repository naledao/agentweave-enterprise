package com.agentweave.conversation.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.agentweave.graphrag.dto.GraphPathResponse;
import com.agentweave.springai.rag.dto.VectorRagCitationResponse;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RagContextMergerTest {

    private final RagContextMerger ragContextMerger = new RagContextMerger();

    @Test
    void mergeVectorCitationsSortsDeduplicatesAndBuildsPromptContext() {
        RagPromptContext context = ragContextMerger.mergeVectorCitations(List.of(
                citation("doc-low", "chunk-low", "source-low", 0.42, "low score snippet"),
                citation("doc-high", "chunk-high", "source-high", 0.91, "high score snippet"),
                citation("doc-high-duplicate", "chunk-high", "source-duplicate", 0.80, "duplicate snippet")));

        assertThat(context.retrievalMode()).isEqualTo("VECTOR_ONLY");
        assertThat(context.citations()).hasSize(2);
        assertThat(context.citations().get(0).chunkId()).isEqualTo("chunk-high");
        assertThat(context.citations().get(0).documentName()).isEqualTo("Runbook chunk-high");
        assertThat(context.citations().get(1).chunkId()).isEqualTo("chunk-low");
        assertThat(context.promptContext())
                .contains("[1] documentId=doc-high, chunkId=chunk-high")
                .contains("high score snippet")
                .contains("[2] documentId=doc-low, chunkId=chunk-low")
                .contains("low score snippet")
                .doesNotContain("duplicate snippet");
    }

    @Test
    void mergeVectorCitationsBuildsFallbackContextWhenNoCitationsExist() {
        RagPromptContext context = ragContextMerger.mergeVectorCitations(List.of());

        assertThat(context.retrievalMode()).isEqualTo("VECTOR_ONLY");
        assertThat(context.citations()).isEmpty();
        assertThat(context.promptContext()).contains("No relevant knowledge base citations");
    }

    @Test
    void mergeVectorCitationsAndGraphPathsProducesHybridPromptContext() {
        GraphPathResponse graphPathHigh = new GraphPathResponse(
                "path-high",
                2,
                List.of("Order Service", "Payment API"),
                List.of("CALLS"),
                List.of("chunk-a", "chunk-b"),
                0.92d);
        GraphPathResponse graphPathDuplicate = new GraphPathResponse(
                "path-high",
                2,
                List.of("Order Service", "Payment API"),
                List.of("CALLS"),
                List.of("chunk-a", "chunk-b"),
                0.61d);
        GraphPathResponse graphPathLow = new GraphPathResponse(
                "path-low",
                1,
                List.of("Order Service", "Inventory Service"),
                List.of("DEPENDS_ON"),
                List.of("chunk-c"),
                0.83d);

        RagPromptContext context = ragContextMerger.merge(
                List.of(
                        citation("doc-1", "chunk-1", "source-1", 0.44d, "low score snippet"),
                        citation("doc-2", "chunk-2", "source-2", 0.88d, "high score snippet"),
                        citation("doc-3", "chunk-2", "source-duplicate", 0.77d, "duplicate snippet")),
                List.of(graphPathLow, graphPathDuplicate, graphPathHigh));

        assertThat(context.retrievalMode()).isEqualTo("HYBRID");
        assertThat(context.citations()).hasSize(2);
        assertThat(context.citations().get(0).chunkId()).isEqualTo("chunk-2");
        assertThat(context.graphPaths()).hasSize(2);
        assertThat(context.graphPaths().get(0).pathId()).isEqualTo("path-high");
        assertThat(context.promptContext())
                .contains("Retrieved knowledge citations:")
                .contains("Retrieved graph paths:")
                .contains("pathId=path-high")
                .contains("sourceChunkIds=chunk-a, chunk-b")
                .contains("entities=Order Service -> Payment API");
    }

    private VectorRagCitationResponse citation(
            String documentId,
            String chunkId,
            String source,
            double score,
            String snippet) {
        return new VectorRagCitationResponse(
                documentId,
                chunkId,
                source,
                "order",
                "RUNBOOK",
                "INTERNAL",
                score,
                snippet,
                Map.of("documentName", "Runbook " + chunkId));
    }
}
