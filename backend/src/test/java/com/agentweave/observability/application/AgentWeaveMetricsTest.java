package com.agentweave.observability.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.agentweave.conversation.domain.ModelCallLogEntity;
import com.agentweave.conversation.domain.ModelCallScenario;
import com.agentweave.conversation.domain.ModelCallStatus;
import com.agentweave.tool.domain.ToolInvocationEntity;
import com.agentweave.tool.domain.ToolInvocationStatus;
import com.agentweave.tool.domain.ToolRiskLevel;
import com.agentweave.tool.domain.ToolType;
import com.agentweave.workflow.domain.AgentRunEntity;
import com.agentweave.workflow.domain.AgentStepEntity;
import com.agentweave.workflow.domain.AgentStepType;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AgentWeaveMetricsTest {

    @Test
    void cancelledModelCallsDoNotIncreaseFailureCounter() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        AgentWeaveMetrics metrics = new AgentWeaveMetrics(meterRegistry);

        metrics.recordModelCall(modelCall(ModelCallStatus.CANCELLED), true);

        assertThat(meterRegistry.find("agentweave.model.call.duration")
                        .tag("status", "CANCELLED")
                        .timer())
                .isNotNull();
        assertThat(meterRegistry.find("agentweave.model.call.failures")
                        .tag("status", "CANCELLED")
                        .counter())
                .isNull();
    }

    @Test
    void failedAndTimedOutModelCallsIncreaseFailureCounter() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        AgentWeaveMetrics metrics = new AgentWeaveMetrics(meterRegistry);

        metrics.recordModelCall(modelCall(ModelCallStatus.FAILED), true);
        metrics.recordModelCall(modelCall(ModelCallStatus.TIMEOUT), true);

        assertThat(meterRegistry.find("agentweave.model.call.failures")
                        .tag("status", "FAILED")
                        .counter()
                        .count())
                .isEqualTo(1);
        assertThat(meterRegistry.find("agentweave.model.call.failures")
                        .tag("status", "TIMEOUT")
                        .counter()
                        .count())
                .isEqualTo(1);
    }

    @Test
    void vectorSearchMetricsRecordDurationCountFailuresAndCitations() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        AgentWeaveMetrics metrics = new AgentWeaveMetrics(meterRegistry);

        metrics.recordVectorSearch("VECTOR_ONLY", "order", "INTERNAL", "SUCCESS", 25, 4, 3);
        metrics.recordVectorSearch("VECTOR_ONLY", "order", "INTERNAL", "FAILED", 10, 0, 0);

        assertThat(meterRegistry.find("agentweave.vector.search.duration")
                        .tag("retrievalMode", "VECTOR_ONLY")
                        .tag("businessDomain", "order")
                        .tag("permissionLevel", "INTERNAL")
                        .tag("status", "SUCCESS")
                        .timer()
                        .count())
                .isEqualTo(1);
        assertThat(meterRegistry.find("agentweave.vector.search.count")
                        .tag("status", "SUCCESS")
                        .counter()
                        .count())
                .isEqualTo(1);
        assertThat(meterRegistry.find("agentweave.vector.search.failures")
                        .tag("status", "FAILED")
                        .counter()
                        .count())
                .isEqualTo(1);
        assertThat(meterRegistry.find("agentweave.vector.search.matches")
                        .tag("status", "SUCCESS")
                        .counter()
                        .count())
                .isEqualTo(4);
        assertThat(meterRegistry.find("agentweave.rag.citation.count")
                        .tag("status", "SUCCESS")
                        .counter()
                        .count())
                .isEqualTo(3);
    }

    @Test
    void graphRagIndexMetricsRecordDurationAndFailures() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        AgentWeaveMetrics metrics = new AgentWeaveMetrics(meterRegistry);

        metrics.recordGraphRagIndex("order", "INTERNAL", "INDEXED", false, 31);
        metrics.recordGraphRagIndex("order", "INTERNAL", "FAILED", true, 12);

        assertThat(meterRegistry.find("agentweave.graphrag.index.duration")
                        .tag("businessDomain", "order")
                        .tag("permissionLevel", "INTERNAL")
                        .tag("status", "INDEXED")
                        .tag("neo4jEnabled", "false")
                        .timer()
                        .count())
                .isEqualTo(1);
        assertThat(meterRegistry.find("agentweave.graphrag.index.failures")
                        .tag("status", "FAILED")
                        .tag("neo4jEnabled", "true")
                        .counter()
                        .count())
                .isEqualTo(1);
    }

    @Test
    void graphRagPathSearchMetricsRecordDurationPathsAndFailures() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        AgentWeaveMetrics metrics = new AgentWeaveMetrics(meterRegistry);

        metrics.recordGraphRagPathSearch("HYBRID", "order", "INTERNAL", "SUCCESS", 44, 3);
        metrics.recordGraphRagPathSearch("GRAPH_ONLY", "order", "INTERNAL", "DEGRADED", 8, 0);

        assertThat(meterRegistry.find("agentweave.graphrag.path.search.duration")
                        .tag("retrievalMode", "HYBRID")
                        .tag("businessDomain", "order")
                        .tag("permissionLevel", "INTERNAL")
                        .tag("status", "SUCCESS")
                        .timer()
                        .count())
                .isEqualTo(1);
        assertThat(meterRegistry.find("agentweave.graphrag.path.count")
                        .tag("status", "SUCCESS")
                        .counter()
                        .count())
                .isEqualTo(3);
        assertThat(meterRegistry.find("agentweave.graphrag.path.search.failures")
                        .tag("status", "DEGRADED")
                        .counter()
                        .count())
                .isEqualTo(1);
    }

    @Test
    void toolCallMetricsRecordDurationAndTerminalCounters() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        AgentWeaveMetrics metrics = new AgentWeaveMetrics(meterRegistry);

        metrics.recordToolCall(toolInvocation(ToolInvocationStatus.SUCCESS, ToolRiskLevel.LOW));
        metrics.recordToolCall(toolInvocation(ToolInvocationStatus.FAILED, ToolRiskLevel.MEDIUM));
        metrics.recordToolCall(toolInvocation(ToolInvocationStatus.DENIED, ToolRiskLevel.HIGH));
        metrics.recordToolCall(toolInvocation(ToolInvocationStatus.TIMEOUT, null));

        assertThat(meterRegistry.find("agentweave.tool.call.duration")
                        .tag("toolCode", "ticket.query")
                        .tag("toolType", "BUSINESS_QUERY")
                        .tag("riskLevel", "LOW")
                        .tag("status", "SUCCESS")
                        .tag("calledBy", "authenticated")
                        .timer()
                        .count())
                .isEqualTo(1);
        assertThat(meterRegistry.find("agentweave.tool.call.failures")
                        .tag("status", "FAILED")
                        .tag("riskLevel", "MEDIUM")
                        .counter()
                        .count())
                .isEqualTo(1);
        assertThat(meterRegistry.find("agentweave.tool.call.denied")
                        .tag("status", "DENIED")
                        .tag("riskLevel", "HIGH")
                        .counter()
                        .count())
                .isEqualTo(1);
        assertThat(meterRegistry.find("agentweave.tool.call.timeout")
                        .tag("status", "TIMEOUT")
                        .tag("riskLevel", "unknown")
                        .counter()
                        .count())
                .isEqualTo(1);
    }

    @Test
    void workflowMetricsRecordRunStepFailuresRetriesAndApprovalWaits() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        AgentWeaveMetrics metrics = new AgentWeaveMetrics(meterRegistry);
        AgentRunEntity run = workflowRun();
        AgentStepEntity failedStep = workflowStep(run, AgentStepType.TOOL_CALL, "tool_node");
        failedStep.start(Instant.now().minusMillis(50));
        failedStep.fail("TOOL_FAILED", "tool failed", Instant.now());
        AgentStepEntity approvalStep = workflowStep(run, AgentStepType.HUMAN_APPROVAL, "human_approval_node");
        approvalStep.start(Instant.now().minusMillis(200));
        approvalStep.succeed("approved", Instant.now());
        AgentStepEntity retryStep = workflowStep(run, AgentStepType.RAG_SEARCH, "rag_node");
        retryStep.recordRetry("retry vector search", Instant.now());

        metrics.recordWorkflowRun(run);
        metrics.recordWorkflowStep(failedStep);
        metrics.recordWorkflowApprovalWait(approvalStep);
        metrics.recordWorkflowStepRetry(retryStep);

        assertThat(meterRegistry.find("agentweave.workflow.run.duration")
                        .tag("workflowType", "AGENT_WORKFLOW")
                        .tag("status", "FAILED")
                        .timer()
                        .count())
                .isEqualTo(1);
        assertThat(meterRegistry.find("agentweave.workflow.run.failures")
                        .tag("status", "FAILED")
                        .counter()
                        .count())
                .isEqualTo(1);
        assertThat(meterRegistry.find("agentweave.workflow.step.duration")
                        .tag("nodeName", "tool_node")
                        .tag("agentRole", "EXECUTOR")
                        .tag("stepType", "TOOL_CALL")
                        .tag("status", "FAILED")
                        .timer()
                        .count())
                .isEqualTo(1);
        assertThat(meterRegistry.find("agentweave.workflow.step.failures")
                        .tag("status", "FAILED")
                        .counter()
                        .count())
                .isEqualTo(1);
        assertThat(meterRegistry.find("agentweave.workflow.step.retry.count")
                        .tag("status", "RETRYING")
                        .counter()
                        .count())
                .isEqualTo(1);
        assertThat(meterRegistry.find("agentweave.workflow.approval.wait.duration")
                        .tag("nodeName", "human_approval_node")
                        .tag("agentRole", "APPROVAL")
                        .tag("stepType", "HUMAN_APPROVAL")
                        .tag("status", "SUCCEEDED")
                        .timer()
                        .count())
                .isEqualTo(1);
    }

    @Test
    void sseMetricsRecordActiveConnectionsEventsDurationsAndFailures() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        AgentWeaveMetrics metrics = new AgentWeaveMetrics(meterRegistry);

        metrics.recordSseConnectionStarted();
        metrics.recordSseEvent("/api/v1/conversations/{conversationId}/stream", "gpt-test", "STREAMING", "message_delta");
        metrics.recordSseFirstTokenDuration(
                "/api/v1/conversations/{conversationId}/stream",
                "gpt-test",
                "STREAMING",
                12);
        metrics.recordSseConnectionFinished(
                "COMPLETED",
                "/api/v1/conversations/{conversationId}/stream",
                "gpt-test",
                34);
        metrics.recordSseConnectionStarted();
        metrics.recordSseConnectionFinished(
                "TIMEOUT",
                "/api/v1/conversations/{conversationId}/stream",
                "gpt-test",
                56);

        assertThat(meterRegistry.find("agentweave.sse.connection.active")
                        .gauge()
                        .value())
                .isZero();
        assertThat(meterRegistry.find("agentweave.sse.event.count")
                        .tag("eventType", "message_delta")
                        .tag("status", "STREAMING")
                        .counter()
                        .count())
                .isEqualTo(1);
        assertThat(meterRegistry.find("agentweave.sse.first_token.duration")
                        .tag("modelName", "gpt-test")
                        .timer()
                        .count())
                .isEqualTo(1);
        assertThat(meterRegistry.find("agentweave.sse.connection.duration")
                        .tag("status", "COMPLETED")
                        .timer()
                        .count())
                .isEqualTo(1);
        assertThat(meterRegistry.find("agentweave.sse.connection.failures")
                        .tag("status", "TIMEOUT")
                        .counter()
                        .count())
                .isEqualTo(1);
    }

    private ModelCallLogEntity modelCall(ModelCallStatus status) {
        return new ModelCallLogEntity(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "openai",
                "mimo-v2.5",
                ModelCallScenario.CHAT_STREAM,
                "prompt",
                "response",
                11,
                7,
                42,
                status,
                null,
                null,
                "trace-" + UUID.randomUUID());
    }

    private ToolInvocationEntity toolInvocation(ToolInvocationStatus status, ToolRiskLevel riskLevel) {
        ToolInvocationEntity invocation = new ToolInvocationEntity(
                UUID.randomUUID(),
                "ticket.query",
                "Ticket Query",
                ToolType.BUSINESS_QUERY,
                riskLevel,
                UUID.randomUUID(),
                "alice",
                null,
                null,
                null,
                null,
                "queryTicket(INC-10001)",
                ToolInvocationStatus.RUNNING,
                "trace-" + UUID.randomUUID());
        Instant finishedAt = Instant.now();
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

    private AgentRunEntity workflowRun() {
        AgentRunEntity run = new AgentRunEntity(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "diagnose payment latency");
        run.setTraceId("trace-" + UUID.randomUUID());
        run.startPlanning(Instant.now().minusSeconds(2));
        run.startExecuting(Instant.now().minusSeconds(1));
        run.fail("WORKFLOW_FAILED", "workflow failed", Instant.now());
        return run;
    }

    private AgentStepEntity workflowStep(AgentRunEntity run, AgentStepType stepType, String nodeName) {
        return new AgentStepEntity(UUID.randomUUID(), run, 1, stepType, nodeName);
    }
}
