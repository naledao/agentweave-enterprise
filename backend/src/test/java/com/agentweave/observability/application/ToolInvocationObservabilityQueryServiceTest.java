package com.agentweave.observability.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.agentweave.observability.dto.ToolInvocationSummaryResponse;
import com.agentweave.shared.security.CurrentUser;
import com.agentweave.shared.security.CurrentUserService;
import com.agentweave.tool.application.ToolInvocationService;
import com.agentweave.tool.domain.ToolInvocationEntity;
import com.agentweave.tool.domain.ToolInvocationStatus;
import com.agentweave.tool.domain.ToolRiskLevel;
import com.agentweave.tool.domain.ToolType;
import com.agentweave.tool.dto.ToolInvocationListResponse;
import com.agentweave.tool.dto.ToolInvocationQueryRequest;
import com.agentweave.tool.repository.ToolInvocationRepository;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

class ToolInvocationObservabilityQueryServiceTest {

    private final ToolInvocationRepository repository = mock(ToolInvocationRepository.class);
    private final ToolInvocationService toolInvocationService = mock(ToolInvocationService.class);
    private final CurrentUserService currentUserService = mock(CurrentUserService.class);
    private final ToolInvocationObservabilityQueryService service = new ToolInvocationObservabilityQueryService(
            repository,
            toolInvocationService,
            currentUserService);

    @Test
    void summaryAggregatesStatusToolAndPagedInvocations() {
        CurrentUser observer = new CurrentUser(
                UUID.randomUUID(),
                "observer",
                "Observer",
                Set.of(),
                Set.of("observability:read"));
        ToolInvocationEntity success = invocation(ToolInvocationStatus.SUCCESS, 20);
        ToolInvocationEntity failed = invocation(ToolInvocationStatus.FAILED, 40);
        ToolInvocationEntity denied = invocation(ToolInvocationStatus.DENIED, 10);
        ToolInvocationQueryRequest request = new ToolInvocationQueryRequest(0, 20, null, null, null, null);
        ToolInvocationListResponse page = new ToolInvocationListResponse(List.of(), 0, 20, 3, 1);

        when(currentUserService.requireCurrentUser()).thenReturn(observer);
        when(repository.findAll(any(Specification.class))).thenReturn(List.of(success, failed, denied));
        when(toolInvocationService.list(request)).thenReturn(page);

        ToolInvocationSummaryResponse response = service.summary(request);

        assertThat(response.total()).isEqualTo(3);
        assertThat(response.success()).isEqualTo(1);
        assertThat(response.failed()).isEqualTo(1);
        assertThat(response.denied()).isEqualTo(1);
        assertThat(response.failureRate()).isEqualTo(1.0d / 3.0d);
        assertThat(response.averageDurationMs()).isEqualTo(0.0d);
        assertThat(response.statusCounts())
                .anySatisfy(status -> {
                    assertThat(status.status()).isEqualTo("success");
                    assertThat(status.count()).isEqualTo(1);
                });
        assertThat(response.toolCounts()).hasSize(1);
        assertThat(response.toolCounts().get(0).toolCode()).isEqualTo("ticket.query");
        assertThat(response.toolCounts().get(0).toolType()).isEqualTo("BUSINESS_QUERY");
        assertThat(response.toolCounts().get(0).failed()).isEqualTo(1);
        assertThat(response.toolCounts().get(0).denied()).isEqualTo(1);
        assertThat(response.invocations()).isSameAs(page);
    }

    private ToolInvocationEntity invocation(ToolInvocationStatus status, long durationMs) {
        ToolInvocationEntity invocation = new ToolInvocationEntity(
                UUID.randomUUID(),
                "ticket.query",
                "Ticket Query",
                ToolType.BUSINESS_QUERY,
                ToolRiskLevel.LOW,
                UUID.randomUUID(),
                "alice",
                null,
                null,
                null,
                null,
                "input",
                ToolInvocationStatus.RUNNING,
                "trace-" + UUID.randomUUID());
        Instant finishedAt = Instant.now().plusMillis(durationMs);
        if (status == ToolInvocationStatus.SUCCESS) {
            invocation.succeed("ok", finishedAt);
        } else if (status == ToolInvocationStatus.FAILED) {
            invocation.fail("failed", finishedAt);
        } else if (status == ToolInvocationStatus.DENIED) {
            invocation.deny("denied", finishedAt);
        } else if (status == ToolInvocationStatus.TIMEOUT) {
            invocation.timeout("timeout", finishedAt);
        }
        return invocation;
    }
}
