package com.agentweave.shared.audit;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AuditSummarySanitizerTest {

    private final AuditSummarySanitizer sanitizer = new AuditSummarySanitizer(new ObjectMapper());

    @Test
    void summarizeMasksSensitiveObjectFields() {
        String summary = sanitizer.summarize(Map.of(
                "username", "alice",
                "password", "secret-password",
                "authorization", "Bearer token-value",
                "nested", Map.of("apiKey", "sk-test")));

        assertThat(summary).contains("\"username\":\"alice\"");
        assertThat(summary).contains("\"password\":\"******\"");
        assertThat(summary).contains("\"authorization\":\"******\"");
        assertThat(summary).contains("\"apiKey\":\"******\"");
        assertThat(summary).doesNotContain("secret-password", "Bearer token-value", "sk-test");
    }

    @Test
    void sanitizeTextMasksAndTruncatesSensitiveValues() {
        String summary = sanitizer.sanitizeText("token=abc123 phone:13800138000 " + "x".repeat(50), 30);

        assertThat(summary).startsWith("token=******");
        assertThat(summary).doesNotContain("abc123", "13800138000");
        assertThat(summary).hasSize(30);
    }
}
