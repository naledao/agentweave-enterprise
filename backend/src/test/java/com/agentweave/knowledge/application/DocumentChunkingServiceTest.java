package com.agentweave.knowledge.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.agentweave.shared.exception.BusinessException;
import java.util.List;
import org.junit.jupiter.api.Test;

class DocumentChunkingServiceTest {

    @Test
    void splitCreatesStableOverlappedChunks() {
        DocumentChunkingService service = new DocumentChunkingService(new DocumentChunkingProperties(10, 3));

        List<String> chunks = service.split("abcdefghij1234567890");

        assertThat(chunks).containsExactly("abcdefghij", "hij1234567", "567890");
    }

    @Test
    void splitPrefersParagraphBoundaryWhenAvailable() {
        DocumentChunkingService service = new DocumentChunkingService(new DocumentChunkingProperties(12, 2));

        List<String> chunks = service.split("alpha\n\nbeta gamma");

        assertThat(chunks.get(0)).isEqualTo("alpha");
        assertThat(String.join("", chunks)).contains("beta").contains("gamma");
    }

    @Test
    void splitRejectsBlankText() {
        DocumentChunkingService service = new DocumentChunkingService(new DocumentChunkingProperties(10, 2));

        assertThatThrownBy(() -> service.split("  "))
                .isInstanceOf(BusinessException.class)
                .hasMessage("cleaned text must not be blank");
    }
}
