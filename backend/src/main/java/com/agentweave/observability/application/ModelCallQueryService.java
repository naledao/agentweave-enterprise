package com.agentweave.observability.application;

import com.agentweave.conversation.domain.ConversationEntity;
import com.agentweave.conversation.domain.ModelCallLogEntity;
import com.agentweave.conversation.domain.ModelCallScenario;
import com.agentweave.conversation.domain.ModelCallStatus;
import com.agentweave.conversation.repository.ModelCallLogRepository;
import com.agentweave.observability.dto.ModelCallListResponse;
import com.agentweave.observability.dto.ModelCallQueryRequest;
import com.agentweave.observability.dto.ModelCallResponse;
import com.agentweave.shared.security.CurrentUser;
import com.agentweave.shared.security.CurrentUserService;
import jakarta.persistence.criteria.Join;
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
public class ModelCallQueryService {

    private static final String OBSERVABILITY_READ = "observability:read";

    private final ModelCallLogRepository modelCallLogRepository;
    private final CurrentUserService currentUserService;

    public ModelCallQueryService(
            ModelCallLogRepository modelCallLogRepository,
            CurrentUserService currentUserService) {
        this.modelCallLogRepository = modelCallLogRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public ModelCallListResponse list(ModelCallQueryRequest request) {
        CurrentUser currentUser = currentUserService.requireCurrentUser();
        Pageable pageable = PageRequest.of(
                request.pageNumber(),
                request.pageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ModelCallResponse> modelCalls = modelCallLogRepository
                .findAll(querySpec(request, currentUser), pageable)
                .map(ModelCallResponse::from);
        return ModelCallListResponse.from(modelCalls);
    }

    private Specification<ModelCallLogEntity> querySpec(ModelCallQueryRequest request, CurrentUser currentUser) {
        Specification<ModelCallLogEntity> spec = readableBy(currentUser);
        spec = and(spec, modelNameEquals(request.normalizedModelName()));
        spec = and(spec, scenarioEquals(request.scenario()));
        spec = and(spec, statusEquals(request.status()));
        spec = and(spec, traceIdEquals(request.normalizedTraceId()));
        spec = and(spec, createdAtGreaterThanOrEqualTo(request.createdFrom()));
        return and(spec, createdAtLessThanOrEqualTo(request.createdTo()));
    }

    private Specification<ModelCallLogEntity> readableBy(CurrentUser user) {
        if (user.hasRole("ADMIN") || user.hasPermission(OBSERVABILITY_READ)) {
            return unrestricted();
        }
        UUID userId = user.id();
        return (root, query, builder) -> {
            Join<ModelCallLogEntity, ConversationEntity> conversation = root.join("conversation");
            return builder.equal(conversation.get("ownerUserId"), userId);
        };
    }

    private Specification<ModelCallLogEntity> modelNameEquals(String modelName) {
        if (modelName == null) {
            return null;
        }
        return (root, query, builder) -> builder.equal(root.get("modelName"), modelName);
    }

    private Specification<ModelCallLogEntity> scenarioEquals(ModelCallScenario scenario) {
        if (scenario == null) {
            return null;
        }
        return (root, query, builder) -> builder.equal(root.get("scenario"), scenario);
    }

    private Specification<ModelCallLogEntity> statusEquals(ModelCallStatus status) {
        if (status == null) {
            return null;
        }
        return (root, query, builder) -> builder.equal(root.get("status"), status);
    }

    private Specification<ModelCallLogEntity> traceIdEquals(String traceId) {
        if (traceId == null) {
            return null;
        }
        return (root, query, builder) -> builder.equal(root.get("traceId"), traceId);
    }

    private Specification<ModelCallLogEntity> createdAtGreaterThanOrEqualTo(Instant createdFrom) {
        if (createdFrom == null) {
            return null;
        }
        return (root, query, builder) -> builder.greaterThanOrEqualTo(root.get("createdAt"), createdFrom);
    }

    private Specification<ModelCallLogEntity> createdAtLessThanOrEqualTo(Instant createdTo) {
        if (createdTo == null) {
            return null;
        }
        return (root, query, builder) -> builder.lessThanOrEqualTo(root.get("createdAt"), createdTo);
    }

    private Specification<ModelCallLogEntity> and(
            Specification<ModelCallLogEntity> left,
            Specification<ModelCallLogEntity> right) {
        if (right == null) {
            return left;
        }
        return left.and(right);
    }

    private Specification<ModelCallLogEntity> unrestricted() {
        return (root, query, builder) -> builder.conjunction();
    }
}
