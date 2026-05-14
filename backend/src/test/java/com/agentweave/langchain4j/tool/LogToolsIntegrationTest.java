package com.agentweave.langchain4j.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.agentweave.auth.domain.PermissionEntity;
import com.agentweave.auth.domain.PermissionType;
import com.agentweave.auth.repository.PermissionRepository;
import com.agentweave.shared.exception.BusinessException;
import com.agentweave.shared.exception.ToolPermissionDeniedException;
import com.agentweave.shared.security.AuthenticatedUser;
import com.agentweave.shared.security.CurrentUser;
import com.agentweave.shared.tracing.CorrelationContext;
import com.agentweave.tool.domain.ToolDefinitionEntity;
import com.agentweave.tool.domain.ToolInvocationEntity;
import com.agentweave.tool.domain.ToolInvocationStatus;
import com.agentweave.tool.domain.ToolRiskLevel;
import com.agentweave.tool.log.LogQueryRequest;
import com.agentweave.tool.log.LogQueryResult;
import com.agentweave.tool.log.LogSearchException;
import com.agentweave.tool.repository.ToolDefinitionRepository;
import com.agentweave.tool.repository.ToolInvocationRepository;
import dev.langchain4j.agent.tool.Tool;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
class LogToolsIntegrationTest {

    @Autowired
    private LogTools logTools;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private ToolDefinitionRepository toolDefinitionRepository;

    @Autowired
    private ToolInvocationRepository toolInvocationRepository;

    @Autowired
    private CorrelationContext correlationContext;

    @BeforeEach
    void setUp() {
        ensurePermission("tool:log:search", "Search logs", PermissionType.TOOL);
        ensureToolDefinition();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        MDC.clear();
    }

    @Test
    void logToolUsesLangChain4jToolAnnotation() throws NoSuchMethodException {
        Tool annotation = LogTools.class
                .getMethod("queryLogs", LogQueryRequest.class)
                .getAnnotation(Tool.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.name()).isEqualTo("query_logs");
        assertThat(annotation.value()).containsExactly(
                "Search recent service logs by service name, keyword, and time range.");
    }

    @Test
    void permittedUserCanQueryLogsAndWriteInvocationLog() {
        authenticate("log_user", Set.of("tool:log:search"));
        String traceId = "trace-log-success-" + UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();

        LogQueryResult result;
        try (CorrelationContext.Scope ignored = correlationContext.open(traceId, conversationId, messageId)) {
            result = logTools.queryLogs(new LogQueryRequest(
                    "chat-service",
                    "ERROR",
                    new LogQueryRequest.TimeRange(
                            Instant.parse("2026-05-13T15:00:00Z"),
                            Instant.parse("2026-05-13T16:00:00Z")),
                    10));
        }

        assertThat(result.hitCount()).isEqualTo(2);
        assertThat(result.summary()).contains("Found 2 log entries");
        assertThat(result.recentErrors()).hasSize(2);
        assertThat(result.recentErrors().get(0).message()).contains("138****5678");
        assertThat(result.recentErrors().get(0).message()).doesNotContain("13812345678");
        assertThat(result.recentErrors().get(1).message()).contains("password=******");
        assertThat(result.recentErrors().get(1).message()).doesNotContain("plain-demo-pass");

        ToolInvocationEntity invocation = latestInvocation(traceId);
        assertThat(invocation.getToolCode()).isEqualTo("log.search");
        assertThat(invocation.getStatus()).isEqualTo(ToolInvocationStatus.SUCCESS);
        assertThat(invocation.getConversationId()).isEqualTo(conversationId);
        assertThat(invocation.getMessageId()).isEqualTo(messageId);
        assertThat(invocation.getResultSummary()).contains("Found 2 log entries");
        assertThat(invocation.getErrorMessage()).isNull();
        assertThat(invocation.getDurationMs()).isNotNull();
    }

    @Test
    void invalidRequestIsRejectedBeforeBusinessExecution() {
        authenticate("log_invalid_user", Set.of("tool:log:search"));
        String traceId = "trace-log-invalid-" + UUID.randomUUID();

        try (CorrelationContext.Scope ignored = correlationContext.open(traceId, null, null)) {
            assertThatThrownBy(() -> logTools.queryLogs(new LogQueryRequest(
                    "",
                    "ERROR",
                    new LogQueryRequest.TimeRange(
                            Instant.parse("2026-05-13T15:00:00Z"),
                            Instant.parse("2026-05-13T16:00:00Z")),
                    10)))
                    .isInstanceOf(ConstraintViolationException.class);
        }

        ToolInvocationEntity invocation = latestInvocation(traceId);
        assertThat(invocation.getToolCode()).isEqualTo("log.search");
        assertThat(invocation.getStatus()).isEqualTo(ToolInvocationStatus.DENIED);
    }

    @Test
    void userWithoutLogPermissionIsDeniedAndLogged() {
        authenticate("log_denied_user", Set.of());
        String traceId = "trace-log-denied-" + UUID.randomUUID();

        try (CorrelationContext.Scope ignored = correlationContext.open(traceId, null, null)) {
            assertThatThrownBy(() -> logTools.queryLogs(validRequest()))
                    .isInstanceOf(ToolPermissionDeniedException.class)
                    .hasMessage("Missing tool permission");
        }

        ToolInvocationEntity invocation = latestInvocation(traceId);
        assertThat(invocation.getToolCode()).isEqualTo("log.search");
        assertThat(invocation.getStatus()).isEqualTo(ToolInvocationStatus.DENIED);
        assertThat(invocation.getErrorMessage()).isEqualTo("Missing tool permission");
    }

    @Test
    void timeRangeLongerThanLimitIsRejectedAndLoggedAsFailure() {
        authenticate("log_range_user", Set.of("tool:log:search"));
        String traceId = "trace-log-range-" + UUID.randomUUID();

        try (CorrelationContext.Scope ignored = correlationContext.open(traceId, null, null)) {
            assertThatThrownBy(() -> logTools.queryLogs(new LogQueryRequest(
                    "chat-service",
                    "ERROR",
                    new LogQueryRequest.TimeRange(
                            Instant.parse("2026-05-12T00:00:00Z"),
                            Instant.parse("2026-05-13T02:00:00Z")),
                    10)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("log query time range cannot exceed 24 hours");
        }

        ToolInvocationEntity invocation = latestInvocation(traceId);
        assertThat(invocation.getToolCode()).isEqualTo("log.search");
        assertThat(invocation.getStatus()).isEqualTo(ToolInvocationStatus.FAILED);
        assertThat(invocation.getErrorMessage()).isEqualTo("log query time range cannot exceed 24 hours");
    }

    @Test
    void downstreamLogServiceFailureReturnsControlledErrorAndWritesFailureLog() {
        authenticate("log_failure_user", Set.of("tool:log:search"));
        String traceId = "trace-log-failure-" + UUID.randomUUID();

        try (CorrelationContext.Scope ignored = correlationContext.open(traceId, null, null)) {
            assertThatThrownBy(() -> logTools.queryLogs(new LogQueryRequest(
                    "log-service",
                    "simulate-downstream-error",
                    new LogQueryRequest.TimeRange(
                            Instant.parse("2026-05-13T15:00:00Z"),
                            Instant.parse("2026-05-13T16:00:00Z")),
                    10)))
                    .isInstanceOf(LogSearchException.class)
                    .hasMessage("log search service unavailable");
        }

        ToolInvocationEntity invocation = latestInvocation(traceId);
        assertThat(invocation.getToolCode()).isEqualTo("log.search");
        assertThat(invocation.getStatus()).isEqualTo(ToolInvocationStatus.FAILED);
        assertThat(invocation.getErrorMessage()).isEqualTo("log search service unavailable");
    }

    private LogQueryRequest validRequest() {
        return new LogQueryRequest(
                "chat-service",
                "ERROR",
                new LogQueryRequest.TimeRange(
                        Instant.parse("2026-05-13T15:00:00Z"),
                        Instant.parse("2026-05-13T16:00:00Z")),
                10);
    }

    private PermissionEntity ensurePermission(String code, String name, PermissionType type) {
        return permissionRepository.findByCode(code)
                .orElseGet(() -> permissionRepository.save(
                        new PermissionEntity(UUID.randomUUID(), code, name, type, null)));
    }

    private ToolDefinitionEntity ensureToolDefinition() {
        return toolDefinitionRepository.findByCode("log.search")
                .map(definition -> {
                    definition.setEnabled(true);
                    definition.setRiskLevel(ToolRiskLevel.MEDIUM);
                    return toolDefinitionRepository.save(definition);
                })
                .orElseGet(() -> toolDefinitionRepository.save(new ToolDefinitionEntity(
                        UUID.randomUUID(),
                        "log.search",
                        "Log search",
                        "Search recent service log snippets.",
                        "tool:log:search",
                        ToolRiskLevel.MEDIUM,
                        true,
                        "{}",
                        "{}")));
    }

    private void authenticate(String username, Set<String> permissions) {
        CurrentUser currentUser = new CurrentUser(
                UUID.randomUUID(),
                username,
                username,
                Set.of("TOOL_TESTER"),
                permissions);
        AuthenticatedUser principal = new AuthenticatedUser(
                currentUser,
                "n/a",
                List.of(),
                true,
                0L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, "n/a", principal.getAuthorities()));
    }

    private ToolInvocationEntity latestInvocation(String traceId) {
        return toolInvocationRepository.findFirstByTraceIdOrderByCreatedAtDesc(traceId)
                .orElseThrow();
    }
}
