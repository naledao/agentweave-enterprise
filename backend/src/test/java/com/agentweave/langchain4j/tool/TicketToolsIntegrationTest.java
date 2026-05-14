package com.agentweave.langchain4j.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.agentweave.auth.domain.PermissionEntity;
import com.agentweave.auth.domain.PermissionType;
import com.agentweave.auth.repository.PermissionRepository;
import com.agentweave.shared.exception.ResourceNotFoundException;
import com.agentweave.shared.exception.ToolPermissionDeniedException;
import com.agentweave.shared.security.AuthenticatedUser;
import com.agentweave.shared.security.CurrentUser;
import com.agentweave.shared.tracing.CorrelationContext;
import com.agentweave.tool.domain.ToolDefinitionEntity;
import com.agentweave.tool.domain.ToolInvocationEntity;
import com.agentweave.tool.domain.ToolInvocationStatus;
import com.agentweave.tool.domain.ToolRiskLevel;
import com.agentweave.tool.repository.ToolDefinitionRepository;
import com.agentweave.tool.repository.ToolInvocationRepository;
import com.agentweave.tool.ticket.TicketQueryRequest;
import com.agentweave.tool.ticket.TicketQueryResult;
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
class TicketToolsIntegrationTest {

    @Autowired
    private TicketTools ticketTools;

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
        ensurePermission("tool:ticket:query", "Query tickets", PermissionType.TOOL);
        ensureToolDefinition();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        MDC.clear();
    }

    @Test
    void ticketToolUsesLangChain4jToolAnnotation() throws NoSuchMethodException {
        Tool annotation = TicketTools.class
                .getMethod("queryTicket", TicketQueryRequest.class)
                .getAnnotation(Tool.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.name()).isEqualTo("query_ticket");
        assertThat(annotation.value()).containsExactly("Query a single ticket summary by ticket number.");
    }

    @Test
    void permittedUserCanQueryTicketAndWriteInvocationLog() {
        authenticate("ticket_user", Set.of("tool:ticket:query"));
        String traceId = "trace-ticket-success-" + UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();

        TicketQueryResult result;
        try (CorrelationContext.Scope ignored = correlationContext.open(traceId, conversationId, messageId)) {
            result = ticketTools.queryTicket(new TicketQueryRequest("INC-10001"));
        }

        assertThat(result.ticketNo()).isEqualTo("INC-10001");
        assertThat(result.title()).contains("Knowledge ingestion");
        assertThat(result.status()).isEqualTo("OPEN");
        assertThat(result.priority()).isEqualTo("P1");
        assertThat(result.assignee()).isEqualTo("Liu Wei");

        ToolInvocationEntity invocation = latestInvocation(traceId);
        assertThat(invocation.getToolCode()).isEqualTo("ticket.query");
        assertThat(invocation.getStatus()).isEqualTo(ToolInvocationStatus.SUCCESS);
        assertThat(invocation.getConversationId()).isEqualTo(conversationId);
        assertThat(invocation.getMessageId()).isEqualTo(messageId);
        assertThat(invocation.getResultSummary()).contains("INC-10001");
        assertThat(invocation.getErrorMessage()).isNull();
    }

    @Test
    void invalidTicketNoIsRejectedBeforeBusinessExecution() {
        authenticate("ticket_invalid_user", Set.of("tool:ticket:query"));
        String traceId = "trace-ticket-invalid-" + UUID.randomUUID();

        try (CorrelationContext.Scope ignored = correlationContext.open(traceId, null, null)) {
            assertThatThrownBy(() -> ticketTools.queryTicket(new TicketQueryRequest("BAD-1")))
                    .isInstanceOf(ConstraintViolationException.class);
        }

        ToolInvocationEntity invocation = latestInvocation(traceId);
        assertThat(invocation.getToolCode()).isEqualTo("ticket.query");
        assertThat(invocation.getStatus()).isEqualTo(ToolInvocationStatus.DENIED);
        assertThat(invocation.getErrorMessage()).contains("must match INC-00000 format");
    }

    @Test
    void userWithoutTicketPermissionIsDeniedAndLogged() {
        authenticate("ticket_denied_user", Set.of());
        String traceId = "trace-ticket-denied-" + UUID.randomUUID();

        try (CorrelationContext.Scope ignored = correlationContext.open(traceId, null, null)) {
            assertThatThrownBy(() -> ticketTools.queryTicket(new TicketQueryRequest("INC-10001")))
                    .isInstanceOf(ToolPermissionDeniedException.class)
                    .hasMessage("Missing tool permission");
        }

        ToolInvocationEntity invocation = latestInvocation(traceId);
        assertThat(invocation.getToolCode()).isEqualTo("ticket.query");
        assertThat(invocation.getStatus()).isEqualTo(ToolInvocationStatus.DENIED);
        assertThat(invocation.getErrorMessage()).isEqualTo("Missing tool permission");
    }

    @Test
    void missingTicketReturnsControlledErrorAndWritesFailureLog() {
        authenticate("ticket_missing_user", Set.of("tool:ticket:query"));
        String traceId = "trace-ticket-missing-" + UUID.randomUUID();

        try (CorrelationContext.Scope ignored = correlationContext.open(traceId, null, null)) {
            assertThatThrownBy(() -> ticketTools.queryTicket(new TicketQueryRequest("INC-99999")))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("ticket not found");
        }

        ToolInvocationEntity invocation = latestInvocation(traceId);
        assertThat(invocation.getToolCode()).isEqualTo("ticket.query");
        assertThat(invocation.getStatus()).isEqualTo(ToolInvocationStatus.FAILED);
        assertThat(invocation.getErrorMessage()).isEqualTo("ticket not found");
    }

    private PermissionEntity ensurePermission(String code, String name, PermissionType type) {
        return permissionRepository.findByCode(code)
                .orElseGet(() -> permissionRepository.save(
                        new PermissionEntity(UUID.randomUUID(), code, name, type, null)));
    }

    private ToolDefinitionEntity ensureToolDefinition() {
        return toolDefinitionRepository.findByCode("ticket.query")
                .map(definition -> {
                    definition.setEnabled(true);
                    definition.setRiskLevel(ToolRiskLevel.LOW);
                    return toolDefinitionRepository.save(definition);
                })
                .orElseGet(() -> toolDefinitionRepository.save(new ToolDefinitionEntity(
                        UUID.randomUUID(),
                        "ticket.query",
                        "Ticket query",
                        "Query a ticket summary by ticket number.",
                        "tool:ticket:query",
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
