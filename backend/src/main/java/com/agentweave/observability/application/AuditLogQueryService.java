package com.agentweave.observability.application;

import com.agentweave.observability.dto.AuditLogListResponse;
import com.agentweave.observability.dto.AuditLogQueryRequest;
import com.agentweave.observability.dto.AuditLogResponse;
import com.agentweave.shared.audit.AuditLogEntity;
import com.agentweave.shared.audit.AuditLogRepository;
import com.agentweave.shared.audit.AuditResult;
import com.agentweave.shared.security.CurrentUser;
import com.agentweave.shared.security.CurrentUserService;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLogQueryService {

    private static final String OBSERVABILITY_READ = "observability:read";

    private final AuditLogRepository auditLogRepository;
    private final CurrentUserService currentUserService;

    public AuditLogQueryService(
            AuditLogRepository auditLogRepository,
            CurrentUserService currentUserService) {
        this.auditLogRepository = auditLogRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public AuditLogListResponse list(AuditLogQueryRequest request) {
        CurrentUser currentUser = currentUserService.requireCurrentUser();
        Pageable pageable = PageRequest.of(
                request.pageNumber(),
                request.pageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AuditLogResponse> auditLogs = auditLogRepository
                .findAll(querySpec(request, currentUser), pageable)
                .map(AuditLogResponse::from);
        return AuditLogListResponse.from(auditLogs);
    }

    private Specification<AuditLogEntity> querySpec(AuditLogQueryRequest request, CurrentUser currentUser) {
        Specification<AuditLogEntity> spec = readableBy(currentUser);
        spec = and(spec, eventTypeEquals(request));
        spec = and(spec, resultEquals(request.result()));
        spec = and(spec, userIdEquals(request.userId()));
        spec = and(spec, resourceTypeEquals(request.normalizedResourceType()));
        spec = and(spec, resourceIdEquals(request.normalizedResourceId()));
        spec = and(spec, traceIdEquals(request.normalizedTraceId()));
        spec = and(spec, createdAtGreaterThanOrEqualTo(request.createdFrom()));
        return and(spec, createdAtLessThanOrEqualTo(request.createdTo()));
    }

    private Specification<AuditLogEntity> readableBy(CurrentUser user) {
        if (user.hasRole("ADMIN") || user.hasPermission(OBSERVABILITY_READ)) {
            return unrestricted();
        }
        return (root, query, builder) -> builder.equal(root.get("userId"), user.id());
    }

    private Specification<AuditLogEntity> eventTypeEquals(AuditLogQueryRequest request) {
        if (request.eventType() == null) {
            return null;
        }
        return (root, query, builder) -> builder.equal(root.get("eventType"), request.eventType());
    }

    private Specification<AuditLogEntity> resultEquals(AuditResult result) {
        if (result == null) {
            return null;
        }
        return (root, query, builder) -> builder.equal(root.get("result"), result);
    }

    private Specification<AuditLogEntity> userIdEquals(UUID userId) {
        if (userId == null) {
            return null;
        }
        return (root, query, builder) -> builder.equal(root.get("userId"), userId);
    }

    private Specification<AuditLogEntity> resourceTypeEquals(String resourceType) {
        if (resourceType == null) {
            return null;
        }
        return (root, query, builder) -> builder.equal(root.get("resourceType"), resourceType);
    }

    private Specification<AuditLogEntity> resourceIdEquals(String resourceId) {
        if (resourceId == null) {
            return null;
        }
        return (root, query, builder) -> builder.equal(root.get("resourceId"), resourceId);
    }

    private Specification<AuditLogEntity> traceIdEquals(String traceId) {
        if (traceId == null) {
            return null;
        }
        return (root, query, builder) -> builder.equal(root.get("traceId"), traceId);
    }

    private Specification<AuditLogEntity> createdAtGreaterThanOrEqualTo(Instant createdFrom) {
        if (createdFrom == null) {
            return null;
        }
        return (root, query, builder) -> builder.greaterThanOrEqualTo(root.get("createdAt"), createdFrom);
    }

    private Specification<AuditLogEntity> createdAtLessThanOrEqualTo(Instant createdTo) {
        if (createdTo == null) {
            return null;
        }
        return (root, query, builder) -> builder.lessThanOrEqualTo(root.get("createdAt"), createdTo);
    }

    private Specification<AuditLogEntity> and(
            Specification<AuditLogEntity> left,
            Specification<AuditLogEntity> right) {
        if (right == null) {
            return left;
        }
        return left.and(right);
    }

    private Specification<AuditLogEntity> unrestricted() {
        return (root, query, builder) -> builder.conjunction();
    }
}
