package com.agentweave.knowledge.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.agentweave.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;

class DocumentCleaningServiceTest {

    private final DocumentCleaningService service = new DocumentCleaningService();

    @Test
    void cleanNormalizesWhitespaceAndPreservesBusinessTokens() {
        DocumentCleanResult result = service.clean(
                "  API path   /api/v1/orders  \r\n\r\n  error code  E1001  \u0007 \n  date   2026-05-12  ");

        assertThat(result.cleanedText()).isEqualTo(
                "API path /api/v1/orders\n\nerror code E1001\ndate 2026-05-12");
        assertThat(result.originalLength()).isGreaterThan(result.textLength());
        assertThat(result.textLength()).isEqualTo(result.cleanedText().length());
    }

    @Test
    void cleanRemovesInvisibleCharactersWithoutDroppingContent() {
        DocumentCleanResult result = service.clean(
                "\uFEFF  heading  \r\n  line one\twith  spaces  \n\n  line two  ");

        assertThat(result.cleanedText()).isEqualTo("heading\nline one with spaces\n\nline two");
        assertThat(result.textLength()).isEqualTo(38);
    }

    @Test
    void cleanRejectsBlankTextAfterNormalization() {
        assertThatThrownBy(() -> service.clean("\u200B\u0000\t"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("cleaned text must not be blank");
    }
}
