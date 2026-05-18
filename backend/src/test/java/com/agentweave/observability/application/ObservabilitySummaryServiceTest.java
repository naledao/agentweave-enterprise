package com.agentweave.observability.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.agentweave.conversation.domain.ModelCallLogEntity;
import com.agentweave.conversation.domain.ModelCallScenario;
import com.agentweave.conversation.domain.ModelCallStatus;
import com.agentweave.conversation.repository.ModelCallLogRepository;
import com.agentweave.observability.dto.GraphRagSummaryResponse;
import com.agentweave.observability.dto.ObservabilitySummaryResponse;
import com.agentweave.observability.dto.ToolInvocationSummaryResponse;
import com.agentweave.shared.security.CurrentUser;
import com.agentweave.shared.security.CurrentUserService;
import com.agentweave.springai.rag.domain.RagRetrievalLog;
import com.agentweave.springai.rag.repository.RagRetrievalLogRepository;
import com.agentweave.tool.dto.ToolInvocationListResponse;
import com.agentweave.workflow.domain.AgentRunEntity;
import com.agentweave.workflow.repository.WorkflowRunRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

class ObservabilitySummaryServiceTest {

    private final ModelCallLogRepository modelCallLogRepository =
            org.mockito.Mockito.mock(ModelCallLogRepository.class);
    private final RagRetrievalLogRepository ragRetrievalLogRepository =
            org.mockito.Mockito.mock(RagRetrievalLogRepository.class);
    private final WorkflowRunRepository workflowRunRepository =
            org.mockito.Mockito.mock(WorkflowRunRepository.class);
    private final GraphRagObservabilityQueryService graphRagService =
            org.mockito.Mockito.mock(GraphRagObservabilityQueryService.class);
    private final ToolInvocationObservabilityQueryService toolService =
            org.mockito.Mockito.mock(ToolInvocationObservabilityQueryService.class);
    private final CurrentUserService currentUserService =
            org.mockito.Mockito.mock(CurrentUserService.class);
    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final HealthEndpoint healthEndpoint = org.mockito.Mockito.mock(HealthEndpoint.class);

    private final ObservabilitySummaryService service = new ObservabilitySummaryService(
            modelCallLogRepository,
            ragRetrievalLogRepository,
            workflowRunRepository,
            graphRagService,
            toolService,
            currentUserService,
            meterRegistry,
            healthEndpoint);

    @Test
    void summaryAggregatesCoreObservabilityData() {
        ModelCallLogEntity modelSuccess = modelCall(ModelCallStatus.SUCCESS, 20);
        ModelCallLogEntity modelFailed = modelCall(ModelCallStatus.FAILED, 40);
        RagRetrievalLog ragSuccess = ragLog();
        ragSuccess.markSuccess(List.of("chunk-1"), List.of(), "count=1", 2);
        AgentRunEntity workflow = workflowRun();
        workflow.startPlanning(Instant.now().minusMillis(50));
        workflow.fail("FAILED", "workflow failed", Instant.now());
        AgentWeaveMetrics metrics = new AgentWeaveMetrics(meterRegistry);
        metrics.recordSseConnectionStarted();
        metrics.recordSseConnectionFinished("TIMEOUT", "/api/v1/conversations/{conversationId}/stream", "mimo-v2.5", 15);

        when(currentUserService.requireCurrentUser()).thenReturn(observer());
        when(modelCallLogRepository.findAll(any(Specification.class))).thenReturn(List.of(modelSuccess, modelFailed));
        when(modelCallLogRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(modelFailed)));
        when(ragRetrievalLogRepository.findAll(any(Specification.class))).thenReturn(List.of(ragSuccess));
        when(ragRetrievalLogRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(ragSuccess)));
        when(workflowRunRepository.findAll(any(Specification.class))).thenReturn(List.of(workflow));
        when(workflowRunRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(workflow)));
        when(graphRagService.summary()).thenReturn(new GraphRagSummaryResponse(null, null, 1, 2));
        when(toolService.summary(any())).thenReturn(emptyToolSummary());
        when(healthEndpoint.health()).thenReturn(Health.up().build());

        ObservabilitySummaryResponse response = service.summary();

        assertThat(response.modelCallSummary().total()).isEqualTo(2);
        assertThat(response.modelCallSummary().failed()).isEqualTo(1);
        assertThat(response.modelCallSummary().averageDurationMs()).isEqualTo(30.0d);
        assertThat(response.ragSummary().citationCount()).isEqualTo(2);
        assertThat(response.workflowSummary().failed()).isEqualTo(1);
        assertThat(response.sseSummary().timedOutConnections()).isEqualTo(1.0d);
        assertThat(response.healthSummary().status()).isEqualTo("UP");
    }

    private CurrentUser observer() {
        return new CurrentUser(
                UUID.randomUUID(),
                "observer",
                "Observer",
                Set.of(),
                Set.of("observability:read"));
    }

    private ModelCallLogEntity modelCall(ModelCallStatus status, long durationMs) {
        return new ModelCallLogEntity(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "openai",
                "mimo-v2.5",
                ModelCallScenario.CHAT_SYNC,
                "prompt",
                "response",
                3,
                5,
                durationMs,
                status,
                null,
                null,
                "trace-" + UUID.randomUUID());
    }

    private RagRetrievalLog ragLog() {
        return new RagRetrievalLog(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                null,
                "trace-" + UUID.randomUUID(),
                "order latency",
                "VECTOR_ONLY",
                java.util.Map.of(),
                "order",
                "RUNBOOK",
                "INTERNAL",
                null,
                null,
                5,
                0.7d);
    }

    private AgentRunEntity workflowRun() {
        AgentRunEntity run = new AgentRunEntity(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "diagnose latency");
        run.setTraceId("trace-" + UUID.randomUUID());
        return run;
    }

    private ToolInvocationSummaryResponse emptyToolSummary() {
        return new ToolInvocationSummaryResponse(
                0,
                0,
                0,
                0,
                0,
                0,
                0.0d,
                0.0d,
                0.0d,
                0.0d,
                List.of(),
                List.of(),
                new ToolInvocationListResponse(List.of(), 0, 10, 0, 0));
    }
}
