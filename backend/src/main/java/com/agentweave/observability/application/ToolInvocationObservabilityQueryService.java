package com.agentweave.observability.application;

import com.agentweave.observability.dto.ToolInvocationStatusCount;
import com.agentweave.observability.dto.ToolInvocationSummaryResponse;
import com.agentweave.observability.dto.ToolInvocationToolCount;
import com.agentweave.shared.security.CurrentUser;
import com.agentweave.shared.security.CurrentUserService;
import com.agentweave.tool.application.ToolInvocationService;
import com.agentweave.tool.domain.ToolInvocationEntity;
import com.agentweave.tool.domain.ToolInvocationStatus;
import com.agentweave.tool.domain.ToolType;
import com.agentweave.tool.dto.ToolInvocationListResponse;
import com.agentweave.tool.dto.ToolInvocationQueryRequest;
import com.agentweave.tool.repository.ToolInvocationRepository;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ToolInvocationObservabilityQueryService {

    private final ToolInvocationRepository toolInvocationRepository;
    private final ToolInvocationService toolInvocationService;
    private final CurrentUserService currentUserService;

    public ToolInvocationObservabilityQueryService(
            ToolInvocationRepository toolInvocationRepository,
            ToolInvocationService toolInvocationService,
            CurrentUserService currentUserService) {
        this.toolInvocationRepository = toolInvocationRepository;
        this.toolInvocationService = toolInvocationService;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public ToolInvocationSummaryResponse summary(ToolInvocationQueryRequest request) {
        CurrentUser user = currentUserService.requireCurrentUser();
        List<ToolInvocationEntity> invocations = toolInvocationRepository.findAll(summarySpec(request, user));
        ToolInvocationListResponse page = toolInvocationService.list(request);
        return response(invocations, page);
    }

    private ToolInvocationSummaryResponse response(
            List<ToolInvocationEntity> invocations,
            ToolInvocationListResponse page) {
        long total = invocations.size();
        EnumMap<ToolInvocationStatus, Long> statusCounts = new EnumMap<>(ToolInvocationStatus.class);
        for (ToolInvocationStatus status : ToolInvocationStatus.values()) {
            statusCounts.put(status, 0L);
        }
        Map<String, ToolAggregate> toolAggregates = new LinkedHashMap<>();
        long durationTotal = 0L;
        long durationCount = 0L;

        for (ToolInvocationEntity invocation : invocations) {
            statusCounts.compute(invocation.getStatus(), (ignored, count) -> count == null ? 1L : count + 1);
            ToolAggregate aggregate = toolAggregates.computeIfAbsent(
                    invocation.getToolCode(),
                    ignored -> new ToolAggregate(
                            invocation.getToolCode(),
                            invocation.getToolName(),
                            invocation.getToolType() == null ? "UNKNOWN" : invocation.getToolType().name()));
            aggregate.accept(invocation);
            if (invocation.getDurationMs() != null) {
                durationTotal += invocation.getDurationMs();
                durationCount++;
            }
        }

        long failed = statusCounts.get(ToolInvocationStatus.FAILED);
        long denied = statusCounts.get(ToolInvocationStatus.DENIED);
        long timeout = statusCounts.get(ToolInvocationStatus.TIMEOUT);
        return new ToolInvocationSummaryResponse(
                total,
                statusCounts.get(ToolInvocationStatus.RUNNING),
                statusCounts.get(ToolInvocationStatus.SUCCESS),
                failed,
                denied,
                timeout,
                rate(failed, total),
                rate(denied, total),
                rate(timeout, total),
                durationCount == 0 ? 0.0d : (double) durationTotal / durationCount,
                statusCounts.entrySet().stream()
                        .map(entry -> new ToolInvocationStatusCount(entry.getKey().value(), entry.getValue()))
                        .toList(),
                toolAggregates.values().stream()
                        .sorted(Comparator.comparingLong(ToolAggregate::count).reversed())
                        .map(ToolAggregate::toResponse)
                        .toList(),
                page);
    }

    private Specification<ToolInvocationEntity> summarySpec(ToolInvocationQueryRequest request, CurrentUser user) {
        Specification<ToolInvocationEntity> spec = readableBy(user);
        spec = and(spec, toolCodeEquals(request.normalizedToolCode()));
        spec = and(spec, toolTypeEquals(request.normalizedToolType()));
        spec = and(spec, statusEquals(request.normalizedStatus()));
        spec = and(spec, createdAtGreaterThanOrEqualTo(request.createdFrom()));
        return and(spec, createdAtLessThanOrEqualTo(request.createdTo()));
    }

    private Specification<ToolInvocationEntity> readableBy(CurrentUser user) {
        if (user.hasRole("ADMIN") || user.hasPermission("observability:read")) {
            return unrestricted();
        }
        return (root, query, builder) -> builder.equal(root.get("userId"), user.id());
    }

    private Specification<ToolInvocationEntity> toolCodeEquals(String toolCode) {
        if (toolCode == null) {
            return null;
        }
        return (root, query, builder) -> builder.equal(root.get("toolCode"), toolCode);
    }

    private Specification<ToolInvocationEntity> toolTypeEquals(ToolType toolType) {
        if (toolType == null) {
            return null;
        }
        return (root, query, builder) -> builder.equal(root.get("toolType"), toolType);
    }

    private Specification<ToolInvocationEntity> statusEquals(ToolInvocationStatus status) {
        if (status == null) {
            return null;
        }
        return (root, query, builder) -> builder.equal(root.get("status"), status);
    }

    private Specification<ToolInvocationEntity> createdAtGreaterThanOrEqualTo(java.time.Instant createdFrom) {
        if (createdFrom == null) {
            return null;
        }
        return (root, query, builder) -> builder.greaterThanOrEqualTo(root.get("createdAt"), createdFrom);
    }

    private Specification<ToolInvocationEntity> createdAtLessThanOrEqualTo(java.time.Instant createdTo) {
        if (createdTo == null) {
            return null;
        }
        return (root, query, builder) -> builder.lessThanOrEqualTo(root.get("createdAt"), createdTo);
    }

    private Specification<ToolInvocationEntity> and(
            Specification<ToolInvocationEntity> left,
            Specification<ToolInvocationEntity> right) {
        if (right == null) {
            return left;
        }
        return left.and(right);
    }

    private Specification<ToolInvocationEntity> unrestricted() {
        return (root, query, builder) -> builder.conjunction();
    }

    private double rate(long count, long total) {
        return total == 0 ? 0.0d : (double) count / total;
    }

    private static final class ToolAggregate {

        private final String toolCode;
        private final String toolName;
        private final String toolType;
        private long count;
        private long failed;
        private long denied;
        private long timeout;
        private long durationTotal;
        private long durationCount;

        private ToolAggregate(String toolCode, String toolName, String toolType) {
            this.toolCode = toolCode;
            this.toolName = toolName;
            this.toolType = toolType;
        }

        private void accept(ToolInvocationEntity invocation) {
            count++;
            if (invocation.getStatus() == ToolInvocationStatus.FAILED) {
                failed++;
            } else if (invocation.getStatus() == ToolInvocationStatus.DENIED) {
                denied++;
            } else if (invocation.getStatus() == ToolInvocationStatus.TIMEOUT) {
                timeout++;
            }
            if (invocation.getDurationMs() != null) {
                durationTotal += invocation.getDurationMs();
                durationCount++;
            }
        }

        private long count() {
            return count;
        }

        private ToolInvocationToolCount toResponse() {
            return new ToolInvocationToolCount(
                    toolCode,
                    toolName,
                    toolType,
                    count,
                    failed,
                    denied,
                    timeout,
                    durationCount == 0 ? 0.0d : (double) durationTotal / durationCount);
        }
    }
}
