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
import com.agentweave.tool.endpoint.EndpointStatusException;
import com.agentweave.tool.endpoint.EndpointStatusRequest;
import com.agentweave.tool.endpoint.EndpointStatusResult;
import com.agentweave.tool.repository.ToolDefinitionRepository;
import com.agentweave.tool.repository.ToolInvocationRepository;
import dev.langchain4j.agent.tool.Tool;
import jakarta.validation.ConstraintViolationException;
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
class EndpointToolsIntegrationTest {

    @Autowired
    private EndpointTools endpointTools;

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
        ensurePermission("tool:api-status:query", "Query API status", PermissionType.TOOL);
        ensureToolDefinition();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        MDC.clear();
    }

    @Test
    void endpointToolUsesLangChain4jToolAnnotation() throws NoSuchMethodException {
        Tool annotation = EndpointTools.class
                .getMethod("queryEndpointStatus", EndpointStatusRequest.class)
                .getAnnotation(Tool.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.name()).isEqualTo("query_endpoint_status");
        assertThat(annotation.value()).containsExactly(
                "Query status metrics for a registered internal endpoint.");
    }

    @Test
    void permittedUserCanQueryRegisteredEndpointAndWriteInvocationLog() {
        authenticate("endpoint_user", Set.of("tool:api-status:query"));
        String traceId = "trace-endpoint-success-" + UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();

        EndpointStatusResult result;
        try (CorrelationContext.Scope ignored = correlationContext.open(traceId, conversationId, messageId)) {
            result = endpointTools.queryEndpointStatus(new EndpointStatusRequest("/api/v1/documents"));
        }

        assertThat(result.endpoint()).isEqualTo("knowledge-service");
        assertThat(result.httpStatus()).isEqualTo(200);
        assertThat(result.averageLatencyMs()).isEqualTo(86);
        assertThat(result.failureRate()).isEqualTo(0.012);
        assertThat(result.checkedAt()).isNotNull();

        ToolInvocationEntity invocation = latestInvocation(traceId);
        assertThat(invocation.getToolCode()).isEqualTo("endpoint.status");
        assertThat(invocation.getStatus()).isEqualTo(ToolInvocationStatus.SUCCESS);
        assertThat(invocation.getConversationId()).isEqualTo(conversationId);
        assertThat(invocation.getMessageId()).isEqualTo(messageId);
        assertThat(invocation.getResultSummary()).contains("knowledge-service");
        assertThat(invocation.getErrorMessage()).isNull();
    }

    @Test
    void unregisteredEndpointIsRejectedAndLoggedAsFailure() {
        authenticate("endpoint_unregistered_user", Set.of("tool:api-status:query"));
        String traceId = "trace-endpoint-unregistered-" + UUID.randomUUID();

        try (CorrelationContext.Scope ignored = correlationContext.open(traceId, null, null)) {
            assertThatThrownBy(() -> endpointTools.queryEndpointStatus(
                    new EndpointStatusRequest("https://example.com/admin")))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("endpoint is not registered");
        }

        ToolInvocationEntity invocation = latestInvocation(traceId);
        assertThat(invocation.getToolCode()).isEqualTo("endpoint.status");
        assertThat(invocation.getStatus()).isEqualTo(ToolInvocationStatus.FAILED);
        assertThat(invocation.getErrorMessage()).isEqualTo("endpoint is not registered");
    }

    @Test
    void invalidEndpointArgumentIsRejectedBeforeBusinessExecution() {
        authenticate("endpoint_invalid_user", Set.of("tool:api-status:query"));
        String traceId = "trace-endpoint-invalid-" + UUID.randomUUID();

        try (CorrelationContext.Scope ignored = correlationContext.open(traceId, null, null)) {
            assertThatThrownBy(() -> endpointTools.queryEndpointStatus(
                    new EndpointStatusRequest("http://internal.local/?token=secret")))
                    .isInstanceOf(ConstraintViolationException.class);
        }

        ToolInvocationEntity invocation = latestInvocation(traceId);
        assertThat(invocation.getToolCode()).isEqualTo("endpoint.status");
        assertThat(invocation.getStatus()).isEqualTo(ToolInvocationStatus.DENIED);
    }

    @Test
    void userWithoutEndpointPermissionIsDeniedAndLogged() {
        authenticate("endpoint_denied_user", Set.of());
        String traceId = "trace-endpoint-denied-" + UUID.randomUUID();

        try (CorrelationContext.Scope ignored = correlationContext.open(traceId, null, null)) {
            assertThatThrownBy(() -> endpointTools.queryEndpointStatus(
                    new EndpointStatusRequest("knowledge-service")))
                    .isInstanceOf(ToolPermissionDeniedException.class)
                    .hasMessage("Missing tool permission");
        }

        ToolInvocationEntity invocation = latestInvocation(traceId);
        assertThat(invocation.getToolCode()).isEqualTo("endpoint.status");
        assertThat(invocation.getStatus()).isEqualTo(ToolInvocationStatus.DENIED);
        assertThat(invocation.getErrorMessage()).isEqualTo("Missing tool permission");
    }

    @Test
    void downstreamStatusServiceFailureReturnsControlledErrorAndWritesFailureLog() {
        authenticate("endpoint_failure_user", Set.of("tool:api-status:query"));
        String traceId = "trace-endpoint-failure-" + UUID.randomUUID();

        try (CorrelationContext.Scope ignored = correlationContext.open(traceId, null, null)) {
            assertThatThrownBy(() -> endpointTools.queryEndpointStatus(
                    new EndpointStatusRequest("status-monitor-down")))
                    .isInstanceOf(EndpointStatusException.class)
                    .hasMessage("endpoint status service unavailable");
        }

        ToolInvocationEntity invocation = latestInvocation(traceId);
        assertThat(invocation.getToolCode()).isEqualTo("endpoint.status");
        assertThat(invocation.getStatus()).isEqualTo(ToolInvocationStatus.FAILED);
        assertThat(invocation.getErrorMessage()).isEqualTo("endpoint status service unavailable");
    }

    private PermissionEntity ensurePermission(String code, String name, PermissionType type) {
        return permissionRepository.findByCode(code)
                .orElseGet(() -> permissionRepository.save(
                        new PermissionEntity(UUID.randomUUID(), code, name, type, null)));
    }

    private ToolDefinitionEntity ensureToolDefinition() {
        return toolDefinitionRepository.findByCode("endpoint.status")
                .map(definition -> {
                    definition.setEnabled(true);
                    definition.setRiskLevel(ToolRiskLevel.LOW);
                    return toolDefinitionRepository.save(definition);
                })
                .orElseGet(() -> toolDefinitionRepository.save(new ToolDefinitionEntity(
                        UUID.randomUUID(),
                        "endpoint.status",
                        "Endpoint status",
                        "Query status metrics for a registered internal endpoint.",
                        "tool:api-status:query",
                        ToolRiskLevel.LOW,
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
