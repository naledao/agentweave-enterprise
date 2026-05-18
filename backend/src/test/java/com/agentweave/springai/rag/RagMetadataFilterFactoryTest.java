package com.agentweave.springai.rag;

import static org.assertj.core.api.Assertions.assertThat;

import com.agentweave.springai.rag.dto.VectorRagSearchRequest;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class RagMetadataFilterFactoryTest {

    private final RagMetadataFilterFactory factory = new RagMetadataFilterFactory();

    @Test
    void describesAndBuildsEffectiveTimeRangeFilter() {
        VectorRagSearchRequest request = new VectorRagSearchRequest(
                "order timeout",
                "order",
                "RUNBOOK",
                "INTERNAL",
                null,
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-12-31T23:59:59Z"),
                5,
                0.0);

        assertThat(factory.describe(request))
                .containsEntry("businessDomain", "order")
                .containsEntry("timeRange", "2026-01-01T00:00:00Z..2026-12-31T23:59:59Z")
                .containsEntry("effectiveFrom", "2026-01-01T00:00:00Z")
                .containsEntry("effectiveTo", "2026-12-31T23:59:59Z");
        assertThat(factory.build(request)).isPresent();
        assertThat(factory.build(request).orElseThrow().toString())
                .contains("effectiveFrom")
                .contains("effectiveTo");
    }
}
