package com.agentweave.observability.application;

import com.agentweave.conversation.domain.ModelCallLogEntity;
import com.agentweave.conversation.domain.ModelCallStatus;
import com.agentweave.conversation.repository.ModelCallLogRepository;
import com.agentweave.observability.dto.GraphRagSummaryResponse;
import com.agentweave.observability.dto.ObservabilitySummaryResponse;
import com.agentweave.observability.dto.ObservabilitySummaryResponse.HealthSummary;
import com.agentweave.observability.dto.ObservabilitySummaryResponse.ModelCallSummary;
import com.agentweave.observability.dto.ObservabilitySummaryResponse.RagSummary;
import com.agentweave.observability.dto.ObservabilitySummaryResponse.SseSummary;
import com.agentweave.observability.dto.ObservabilitySummaryResponse.WorkflowSummary;
import com.agentweave.shared.security.CurrentUser;
import com.agentweave.shared.security.CurrentUserService;
import com.agentweave.springai.rag.domain.RagRetrievalLog;
import com.agentweave.springai.rag.domain.RagRetrievalStatus;
import com.agentweave.springai.rag.repository.RagRetrievalLogRepository;
import com.agentweave.tool.dto.ToolInvocationQueryRequest;
import com.agentweave.workflow.domain.AgentRunEntity;
import com.agentweave.workflow.domain.WorkflowRunStatus;
import com.agentweave.workflow.repository.WorkflowRunRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.actuate.health.CompositeHealth;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.SystemHealth;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ObservabilitySummaryService {

    private static final String OBSERVABILITY_READ = "observability:read";

    private final ModelCallLogRepository modelCallLogRepository;
    private final RagRetrievalLogRepository ragRetrievalLogRepository;
    private final WorkflowRunRepository workflowRunRepository;
    private final GraphRagObservabilityQueryService graphRagObservabilityQueryService;
    private final ToolInvocationObservabilityQueryService toolInvocationObservabilityQueryService;
    private final CurrentUserService currentUserService;
    private final MeterRegistry meterRegistry;
    private final HealthEndpoint healthEndpoint;

    public ObservabilitySummaryService(
            ModelCallLogRepository modelCallLogRepository,
            RagRetrievalLogRepository ragRetrievalLogRepository,
            WorkflowRunRepository workflowRunRepository,
            GraphRagObservabilityQueryService graphRagObservabilityQueryService,
            ToolInvocationObservabilityQueryService toolInvocationObservabilityQueryService,
            CurrentUserService currentUserService,
            MeterRegistry meterRegistry,
            HealthEndpoint healthEndpoint) {
        this.modelCallLogRepository = modelCallLogRepository;
        this.ragRetrievalLogRepository = ragRetrievalLogRepository;
        this.workflowRunRepository = workflowRunRepository;
        this.graphRagObservabilityQueryService = graphRagObservabilityQueryService;
        this.toolInvocationObservabilityQueryService = toolInvocationObservabilityQueryService;
        this.currentUserService = currentUserService;
        this.meterRegistry = meterRegistry;
        this.healthEndpoint = healthEndpoint;
    }

    @Transactional(readOnly = true)
    public ObservabilitySummaryResponse summary() {
        CurrentUser currentUser = currentUserService.requireCurrentUser();
        GraphRagSummaryResponse graphRagSummary = graphRagObservabilityQueryService.summary();
        return new ObservabilitySummaryResponse(
                modelCallSummary(modelCallSpec(currentUser)),
                ragSummary(ragSpec(currentUser)),
                graphRagSummary,
                toolInvocationObservabilityQueryService.summary(new ToolInvocationQueryRequest(0, 10, null, null, null, null, null)),
                workflowSummary(workflowSpec(currentUser)),
                sseSummary(),
                healthSummary());
    }

    private ModelCallSummary modelCallSummary(Specification<ModelCallLogEntity> spec) {
        List<ModelCallLogEntity> logs = modelCallLogRepository.findAll(spec);
        long total = logs.size();
        long failed = logs.stream().filter(log -> log.getStatus() == ModelCallStatus.FAILED).count();
        long timedOut = logs.stream().filter(log -> log.getStatus() == ModelCallStatus.TIMEOUT).count();
        double averageDurationMs = logs.stream()
                .mapToLong(ModelCallLogEntity::getDurationMs)
                .average()
                .orElse(0.0d);
        Instant latestCreatedAt = modelCallLogRepository.findAll(
                        spec,
                        PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "createdAt")))
                .stream()
                .findFirst()
                .map(ModelCallLogEntity::getCreatedAt)
                .orElse(null);
        return new ModelCallSummary(
                total,
                failed,
                timedOut,
                rate(failed, total),
                rate(timedOut, total),
                averageDurationMs,
                latestCreatedAt);
    }

    private RagSummary ragSummary(Specification<RagRetrievalLog> spec) {
        List<RagRetrievalLog> logs = ragRetrievalLogRepository.findAll(spec);
        long total = logs.size();
        long successful = logs.stream().filter(log -> log.getStatus() == RagRetrievalStatus.SUCCESS).count();
        long failed = logs.stream().filter(log -> log.getStatus() == RagRetrievalStatus.FAILED).count();
        long degraded = logs.stream().filter(log -> log.getStatus() == RagRetrievalStatus.DEGRADED).count();
        double averageDurationMs = logs.stream()
                .mapToLong(RagRetrievalLog::getDurationMs)
                .average()
                .orElse(0.0d);
        long citationCount = logs.stream()
                .mapToLong(RagRetrievalLog::getCitationCount)
                .sum();
        Instant latestCreatedAt = ragRetrievalLogRepository.findAll(
                        spec,
                        PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "createdAt")))
                .stream()
                .findFirst()
                .map(RagRetrievalLog::getCreatedAt)
                .orElse(null);
        return new RagSummary(
                total,
                successful,
                failed,
                degraded,
                rate(failed, total),
                averageDurationMs,
                citationCount,
                latestCreatedAt);
    }

    private WorkflowSummary workflowSummary(Specification<AgentRunEntity> spec) {
        List<AgentRunEntity> runs = workflowRunRepository.findAll(spec);
        long total = runs.size();
        long running = runs.stream()
                .filter(run -> run.getStatus() == WorkflowRunStatus.PLANNING
                        || run.getStatus() == WorkflowRunStatus.EXECUTING
                        || run.getStatus() == WorkflowRunStatus.REVIEWING
                        || run.getStatus() == WorkflowRunStatus.WAITING_APPROVAL)
                .count();
        long succeeded = runs.stream().filter(run -> run.getStatus() == WorkflowRunStatus.SUCCEEDED).count();
        long failed = runs.stream().filter(run -> run.getStatus() == WorkflowRunStatus.FAILED).count();
        long cancelled = runs.stream().filter(run -> run.getStatus() == WorkflowRunStatus.CANCELLED).count();
        double averageDurationMs = runs.stream()
                .filter(run -> run.getStartedAt() != null && run.getFinishedAt() != null)
                .mapToLong(run -> Duration.between(run.getStartedAt(), run.getFinishedAt()).toMillis())
                .average()
                .orElse(0.0d);
        Instant latestCreatedAt = workflowRunRepository.findAll(
                        spec,
                        PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "createdAt")))
                .stream()
                .findFirst()
                .map(AgentRunEntity::getCreatedAt)
                .orElse(null);
        return new WorkflowSummary(
                total,
                running,
                succeeded,
                failed,
                cancelled,
                rate(failed, total),
                averageDurationMs,
                latestCreatedAt);
    }

    private SseSummary sseSummary() {
        Timer connectionDuration = meterRegistry.find("agentweave.sse.connection.duration").timer();
        Timer firstTokenDuration = meterRegistry.find("agentweave.sse.first_token.duration").timer();
        return new SseSummary(
                gaugeValue("agentweave.sse.connection.active"),
                timerCount("agentweave.sse.connection.duration", "status", "COMPLETED"),
                counterValue("agentweave.sse.connection.failures", "status", "FAILED"),
                counterValue("agentweave.sse.connection.failures", "status", "TIMEOUT"),
                connectionDuration == null ? 0.0d : connectionDuration.mean(java.util.concurrent.TimeUnit.MILLISECONDS),
                firstTokenDuration == null ? 0.0d : firstTokenDuration.mean(java.util.concurrent.TimeUnit.MILLISECONDS));
    }

    private HealthSummary healthSummary() {
        HealthComponent health = healthEndpoint.health();
        Map<String, String> components = new LinkedHashMap<>();
        if (health instanceof CompositeHealth compositeHealth) {
            compositeHealth.getComponents()
                    .forEach((name, component) -> components.put(name, component.getStatus().getCode()));
        }
        List<String> groups = health instanceof SystemHealth systemHealth
                ? systemHealth.getGroups().stream().sorted().toList()
                : List.of();
        return new HealthSummary(health.getStatus().getCode(), components, groups);
    }

    private Specification<ModelCallLogEntity> modelCallSpec(CurrentUser user) {
        if (canReadAll(user)) {
            return unrestricted();
        }
        return (root, query, builder) -> builder.equal(root.join("conversation").get("ownerUserId"), user.id());
    }

    private Specification<RagRetrievalLog> ragSpec(CurrentUser user) {
        if (canReadAll(user)) {
            return unrestricted();
        }
        return (root, query, builder) -> builder.equal(root.join("conversation").get("ownerUserId"), user.id());
    }

    private Specification<AgentRunEntity> workflowSpec(CurrentUser user) {
        if (canReadAll(user)) {
            return unrestricted();
        }
        return (root, query, builder) -> builder.equal(root.get("userId"), user.id());
    }

    private boolean canReadAll(CurrentUser user) {
        return user.hasRole("ADMIN") || user.hasPermission(OBSERVABILITY_READ);
    }

    private <T> Specification<T> unrestricted() {
        return (root, query, builder) -> builder.conjunction();
    }

    private double gaugeValue(String meterName) {
        io.micrometer.core.instrument.Gauge gauge = meterRegistry.find(meterName).gauge();
        return gauge == null ? 0.0d : gauge.value();
    }

    private double counterValue(String meterName, String tagKey, String tagValue) {
        io.micrometer.core.instrument.Counter counter = meterRegistry.find(meterName).tag(tagKey, tagValue).counter();
        return counter == null ? 0.0d : counter.count();
    }

    private double timerCount(String meterName, String tagKey, String tagValue) {
        Timer timer = meterRegistry.find(meterName).tag(tagKey, tagValue).timer();
        return timer == null ? 0.0d : timer.count();
    }

    private double rate(long count, long total) {
        return total == 0 ? 0.0d : (double) count / total;
    }
}
