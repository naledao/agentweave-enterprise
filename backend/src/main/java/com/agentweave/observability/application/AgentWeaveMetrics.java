package com.agentweave.observability.application;

import com.agentweave.conversation.domain.ModelCallLogEntity;
import com.agentweave.conversation.domain.ModelCallStatus;
import com.agentweave.tool.domain.ToolInvocationEntity;
import com.agentweave.tool.domain.ToolInvocationStatus;
import com.agentweave.workflow.domain.AgentRole;
import com.agentweave.workflow.domain.AgentRunEntity;
import com.agentweave.workflow.domain.AgentStepEntity;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

@Component
public class AgentWeaveMetrics {

    private static final String MODEL_CALL_DURATION = "agentweave.model.call.duration";
    private static final String MODEL_CALL_FAILURES = "agentweave.model.call.failures";
    private static final String MODEL_TOKEN_INPUT = "agentweave.model.token.input";
    private static final String MODEL_TOKEN_OUTPUT = "agentweave.model.token.output";
    private static final String MODEL_TOKEN_TOTAL = "agentweave.model.token.total";
    private static final String VECTOR_SEARCH_DURATION = "agentweave.vector.search.duration";
    private static final String VECTOR_SEARCH_COUNT = "agentweave.vector.search.count";
    private static final String VECTOR_SEARCH_FAILURES = "agentweave.vector.search.failures";
    private static final String VECTOR_SEARCH_MATCHES = "agentweave.vector.search.matches";
    private static final String RAG_CITATION_COUNT = "agentweave.rag.citation.count";
    private static final String GRAPHRAG_INDEX_DURATION = "agentweave.graphrag.index.duration";
    private static final String GRAPHRAG_INDEX_FAILURES = "agentweave.graphrag.index.failures";
    private static final String GRAPHRAG_PATH_SEARCH_DURATION = "agentweave.graphrag.path.search.duration";
    private static final String GRAPHRAG_PATH_COUNT = "agentweave.graphrag.path.count";
    private static final String GRAPHRAG_PATH_SEARCH_FAILURES = "agentweave.graphrag.path.search.failures";
    private static final String TOOL_CALL_DURATION = "agentweave.tool.call.duration";
    private static final String TOOL_CALL_FAILURES = "agentweave.tool.call.failures";
    private static final String TOOL_CALL_DENIED = "agentweave.tool.call.denied";
    private static final String TOOL_CALL_TIMEOUT = "agentweave.tool.call.timeout";
    private static final String WORKFLOW_RUN_DURATION = "agentweave.workflow.run.duration";
    private static final String WORKFLOW_RUN_FAILURES = "agentweave.workflow.run.failures";
    private static final String WORKFLOW_STEP_DURATION = "agentweave.workflow.step.duration";
    private static final String WORKFLOW_STEP_FAILURES = "agentweave.workflow.step.failures";
    private static final String WORKFLOW_STEP_RETRY_COUNT = "agentweave.workflow.step.retry.count";
    private static final String WORKFLOW_APPROVAL_WAIT_DURATION = "agentweave.workflow.approval.wait.duration";
    private static final String SSE_CONNECTION_DURATION = "agentweave.sse.connection.duration";
    private static final String SSE_CONNECTION_ACTIVE = "agentweave.sse.connection.active";
    private static final String SSE_CONNECTION_FAILURES = "agentweave.sse.connection.failures";
    private static final String SSE_FIRST_TOKEN_DURATION = "agentweave.sse.first_token.duration";
    private static final String SSE_EVENT_COUNT = "agentweave.sse.event.count";

    private final MeterRegistry meterRegistry;
    private final AtomicInteger activeSseConnections = new AtomicInteger();

    public AgentWeaveMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        Gauge.builder(SSE_CONNECTION_ACTIVE, activeSseConnections, AtomicInteger::get)
                .description("Active SSE chat stream connections")
                .register(meterRegistry);
    }

    public void recordModelCall(ModelCallLogEntity log, boolean streaming) {
        Tags tags = Tags.of(
                "provider", safeTag(log.getProvider()),
                "modelName", safeTag(log.getModelName()),
                "scenario", log.getScenario().name(),
                "status", log.getStatus().name(),
                "streaming", Boolean.toString(streaming));
        Timer.builder(MODEL_CALL_DURATION)
                .tags(tags)
                .register(meterRegistry)
                .record(Duration.ofMillis(Math.max(0, log.getDurationMs())));
        if (countsAsFailure(log.getStatus())) {
            Counter.builder(MODEL_CALL_FAILURES)
                    .tags(tags)
                    .register(meterRegistry)
                    .increment();
        }
        incrementTokenCounter(MODEL_TOKEN_INPUT, tags, log.getInputTokens());
        incrementTokenCounter(MODEL_TOKEN_OUTPUT, tags, log.getOutputTokens());
        incrementTokenCounter(MODEL_TOKEN_TOTAL, tags, log.getTotalTokens());
    }

    public void recordVectorSearch(
            String retrievalMode,
            String businessDomain,
            String permissionLevel,
            String status,
            long durationMs,
            int matchedCount,
            int citationCount) {
        Tags tags = Tags.of(
                "retrievalMode", safeTag(retrievalMode),
                "businessDomain", safeTag(businessDomain),
                "permissionLevel", safeTag(permissionLevel),
                "status", safeTag(status));
        Timer.builder(VECTOR_SEARCH_DURATION)
                .tags(tags)
                .register(meterRegistry)
                .record(Duration.ofMillis(Math.max(0, durationMs)));
        Counter.builder(VECTOR_SEARCH_COUNT)
                .tags(tags)
                .register(meterRegistry)
                .increment();
        if (countsAsVectorSearchFailure(status)) {
            Counter.builder(VECTOR_SEARCH_FAILURES)
                    .tags(tags)
                    .register(meterRegistry)
                    .increment();
        }
        if (matchedCount > 0) {
            Counter.builder(VECTOR_SEARCH_MATCHES)
                    .tags(tags)
                    .register(meterRegistry)
                    .increment(matchedCount);
        }
        if (citationCount > 0) {
            Counter.builder(RAG_CITATION_COUNT)
                    .tags(tags)
                    .register(meterRegistry)
                    .increment(citationCount);
        }
    }

    public void recordGraphRagIndex(
            String businessDomain,
            String permissionLevel,
            String status,
            boolean neo4jEnabled,
            long durationMs) {
        Tags tags = Tags.of(
                "businessDomain", safeTag(businessDomain),
                "permissionLevel", safeTag(permissionLevel),
                "status", safeTag(status),
                "neo4jEnabled", Boolean.toString(neo4jEnabled));
        Timer.builder(GRAPHRAG_INDEX_DURATION)
                .tags(tags)
                .register(meterRegistry)
                .record(Duration.ofMillis(Math.max(0, durationMs)));
        if (countsAsGraphRagFailure(status)) {
            Counter.builder(GRAPHRAG_INDEX_FAILURES)
                    .tags(tags)
                    .register(meterRegistry)
                    .increment();
        }
    }

    public void recordGraphRagPathSearch(
            String retrievalMode,
            String businessDomain,
            String permissionLevel,
            String status,
            long durationMs,
            int pathCount) {
        Tags tags = Tags.of(
                "retrievalMode", safeTag(retrievalMode),
                "businessDomain", safeTag(businessDomain),
                "permissionLevel", safeTag(permissionLevel),
                "status", safeTag(status));
        Timer.builder(GRAPHRAG_PATH_SEARCH_DURATION)
                .tags(tags)
                .register(meterRegistry)
                .record(Duration.ofMillis(Math.max(0, durationMs)));
        if (pathCount > 0) {
            Counter.builder(GRAPHRAG_PATH_COUNT)
                    .tags(tags)
                    .register(meterRegistry)
                    .increment(pathCount);
        }
        if (countsAsGraphRagFailure(status)) {
            Counter.builder(GRAPHRAG_PATH_SEARCH_FAILURES)
                    .tags(tags)
                    .register(meterRegistry)
                    .increment();
        }
    }

    public void recordToolCall(ToolInvocationEntity invocation) {
        Tags tags = Tags.of(
                "toolCode", safeTag(invocation.getToolCode()),
                "toolType", invocation.getToolType() == null ? "UNKNOWN" : invocation.getToolType().name(),
                "riskLevel", invocation.getRiskLevel() == null ? "unknown" : invocation.getRiskLevel().name(),
                "status", invocation.getStatus().name(),
                "calledBy", safeCalledBy(invocation.getUsername()));
        Timer.builder(TOOL_CALL_DURATION)
                .tags(tags)
                .register(meterRegistry)
                .record(Duration.ofMillis(Math.max(0, safeDuration(invocation))));
        if (invocation.getStatus() == ToolInvocationStatus.FAILED) {
            Counter.builder(TOOL_CALL_FAILURES)
                    .tags(tags)
                    .register(meterRegistry)
                    .increment();
        }
        if (invocation.getStatus() == ToolInvocationStatus.DENIED) {
            Counter.builder(TOOL_CALL_DENIED)
                    .tags(tags)
                    .register(meterRegistry)
                    .increment();
        }
        if (invocation.getStatus() == ToolInvocationStatus.TIMEOUT) {
            Counter.builder(TOOL_CALL_TIMEOUT)
                    .tags(tags)
                    .register(meterRegistry)
                    .increment();
        }
    }

    public void recordWorkflowRun(AgentRunEntity run) {
        Tags tags = Tags.of(
                "workflowType", "AGENT_WORKFLOW",
                "status", run.getStatus().name());
        Timer.builder(WORKFLOW_RUN_DURATION)
                .tags(tags)
                .register(meterRegistry)
                .record(Duration.ofMillis(Math.max(0, workflowRunDuration(run))));
        if ("FAILED".equals(run.getStatus().name())) {
            Counter.builder(WORKFLOW_RUN_FAILURES)
                    .tags(tags)
                    .register(meterRegistry)
                    .increment();
        }
    }

    public void recordWorkflowStep(AgentStepEntity step) {
        Tags tags = workflowStepTags(step);
        Timer.builder(WORKFLOW_STEP_DURATION)
                .tags(tags)
                .register(meterRegistry)
                .record(Duration.ofMillis(Math.max(0, safeDuration(step))));
        if ("FAILED".equals(step.getStatus().name())) {
            Counter.builder(WORKFLOW_STEP_FAILURES)
                    .tags(tags)
                    .register(meterRegistry)
                    .increment();
        }
    }

    public void recordWorkflowStepRetry(AgentStepEntity step) {
        Counter.builder(WORKFLOW_STEP_RETRY_COUNT)
                .tags(workflowStepTags(step))
                .register(meterRegistry)
                .increment();
    }

    public void recordWorkflowApprovalWait(AgentStepEntity step) {
        Timer.builder(WORKFLOW_APPROVAL_WAIT_DURATION)
                .tags(workflowStepTags(step))
                .register(meterRegistry)
                .record(Duration.ofMillis(Math.max(0, safeDuration(step))));
    }

    public void recordSseConnectionStarted() {
        activeSseConnections.incrementAndGet();
    }

    public void recordSseConnectionFinished(
            String status,
            String endpoint,
            String modelName,
            long durationMs) {
        activeSseConnections.updateAndGet(value -> Math.max(0, value - 1));
        Tags tags = sseTags(status, endpoint, modelName);
        Timer.builder(SSE_CONNECTION_DURATION)
                .tags(tags)
                .register(meterRegistry)
                .record(Duration.ofMillis(Math.max(0, durationMs)));
        if (countsAsSseFailure(status)) {
            Counter.builder(SSE_CONNECTION_FAILURES)
                    .tags(tags)
                    .register(meterRegistry)
                    .increment();
        }
    }

    public void recordSseFirstTokenDuration(
            String endpoint,
            String modelName,
            String status,
            long durationMs) {
        Timer.builder(SSE_FIRST_TOKEN_DURATION)
                .tags(sseTags(status, endpoint, modelName))
                .register(meterRegistry)
                .record(Duration.ofMillis(Math.max(0, durationMs)));
    }

    public void recordSseEvent(
            String endpoint,
            String modelName,
            String status,
            String eventType) {
        Counter.builder(SSE_EVENT_COUNT)
                .tags(sseTags(status, endpoint, modelName).and("eventType", safeTag(eventType)))
                .register(meterRegistry)
                .increment();
    }

    private void incrementTokenCounter(String name, Tags tags, Integer tokens) {
        if (tokens == null || tokens <= 0) {
            return;
        }
        Counter.builder(name)
                .tags(tags)
                .register(meterRegistry)
                .increment(tokens);
    }

    private boolean countsAsFailure(ModelCallStatus status) {
        return status == ModelCallStatus.FAILED || status == ModelCallStatus.TIMEOUT;
    }

    private boolean countsAsVectorSearchFailure(String status) {
        return "FAILED".equals(status);
    }

    private boolean countsAsGraphRagFailure(String status) {
        return "FAILED".equals(status) || "DEGRADED".equals(status);
    }

    private boolean countsAsSseFailure(String status) {
        return "FAILED".equals(status) || "TIMEOUT".equals(status) || "CLIENT_DISCONNECTED".equals(status);
    }

    private long safeDuration(ToolInvocationEntity invocation) {
        Long durationMs = invocation.getDurationMs();
        return durationMs == null ? 0L : durationMs;
    }

    private long safeDuration(AgentStepEntity step) {
        Long durationMs = step.getDurationMs();
        return durationMs == null ? 0L : durationMs;
    }

    private long workflowRunDuration(AgentRunEntity run) {
        if (run.getStartedAt() == null || run.getFinishedAt() == null) {
            return 0L;
        }
        return Duration.between(run.getStartedAt(), run.getFinishedAt()).toMillis();
    }

    private Tags workflowStepTags(AgentStepEntity step) {
        AgentRole agentRole = step.getAgentRole();
        return Tags.of(
                "workflowType", "AGENT_WORKFLOW",
                "nodeName", safeTag(step.getNodeName()),
                "agentRole", agentRole == null ? "SYSTEM" : agentRole.name(),
                "stepType", step.getStepType().name(),
                "status", step.getStatus().name());
    }

    private Tags sseTags(String status, String endpoint, String modelName) {
        return Tags.of(
                "status", safeTag(status),
                "endpoint", safeTag(endpoint),
                "modelName", safeTag(modelName));
    }

    private String safeCalledBy(String username) {
        if (username == null || username.isBlank()) {
            return "unknown";
        }
        return "authenticated";
    }

    private String safeTag(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value.trim();
    }
}
