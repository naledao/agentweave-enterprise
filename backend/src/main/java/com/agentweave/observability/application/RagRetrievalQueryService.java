package com.agentweave.observability.application;

import com.agentweave.conversation.domain.ConversationEntity;
import com.agentweave.observability.dto.RagRetrievalListResponse;
import com.agentweave.observability.dto.RagRetrievalQueryRequest;
import com.agentweave.observability.dto.RagRetrievalResponse;
import com.agentweave.shared.exception.ResourceNotFoundException;
import com.agentweave.shared.security.CurrentUser;
import com.agentweave.shared.security.CurrentUserService;
import com.agentweave.springai.rag.domain.RagRetrievalLog;
import com.agentweave.springai.rag.domain.RagRetrievalStatus;
import com.agentweave.springai.rag.repository.RagRetrievalLogRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
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
public class RagRetrievalQueryService {

    private static final String OBSERVABILITY_READ = "observability:read";

    private final RagRetrievalLogRepository ragRetrievalLogRepository;
    private final CurrentUserService currentUserService;

    public RagRetrievalQueryService(
            RagRetrievalLogRepository ragRetrievalLogRepository,
            CurrentUserService currentUserService) {
        this.ragRetrievalLogRepository = ragRetrievalLogRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public RagRetrievalListResponse list(RagRetrievalQueryRequest request) {
        CurrentUser currentUser = currentUserService.requireCurrentUser();
        Pageable pageable = PageRequest.of(
                request.pageNumber(),
                request.pageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<RagRetrievalResponse> retrievals = ragRetrievalLogRepository
                .findAll(querySpec(request, currentUser), pageable)
                .map(RagRetrievalResponse::from);
        return RagRetrievalListResponse.from(retrievals);
    }

    @Transactional(readOnly = true)
    public RagRetrievalResponse get(UUID id) {
        CurrentUser currentUser = currentUserService.requireCurrentUser();
        RagRetrievalLog log = ragRetrievalLogRepository
                .findOne(and(idEquals(id), readableBy(currentUser)))
                .orElseThrow(() -> new ResourceNotFoundException("RAG retrieval log not found"));
        return RagRetrievalResponse.from(log);
    }

    private Specification<RagRetrievalLog> querySpec(
            RagRetrievalQueryRequest request,
            CurrentUser currentUser) {
        Specification<RagRetrievalLog> spec = readableBy(currentUser);
        spec = and(spec, retrievalModeEquals(request.normalizedRetrievalMode()));
        spec = and(spec, businessDomainEquals(request.normalizedBusinessDomain()));
        spec = and(spec, documentTypeEquals(request.normalizedDocumentType()));
        spec = and(spec, permissionLevelEquals(request.normalizedPermissionLevel()));
        spec = and(spec, statusEquals(request.status()));
        spec = and(spec, conversationIdEquals(request.conversationId()));
        spec = and(spec, messageIdEquals(request.messageId()));
        spec = and(spec, workflowRunIdEquals(request.workflowRunId()));
        spec = and(spec, workflowStepIdEquals(request.workflowStepId()));
        spec = and(spec, traceIdEquals(request.normalizedTraceId()));
        spec = and(spec, createdAtGreaterThanOrEqualTo(request.createdFrom()));
        return and(spec, createdAtLessThanOrEqualTo(request.createdTo()));
    }

    private Specification<RagRetrievalLog> readableBy(CurrentUser user) {
        if (user.hasRole("ADMIN") || user.hasPermission(OBSERVABILITY_READ)) {
            return unrestricted();
        }
        UUID userId = user.id();
        return (root, query, builder) -> {
            Join<RagRetrievalLog, ConversationEntity> conversation = root.join("conversation", JoinType.LEFT);
            return builder.equal(conversation.get("ownerUserId"), userId);
        };
    }

    private Specification<RagRetrievalLog> idEquals(UUID id) {
        return (root, query, builder) -> builder.equal(root.get("id"), id);
    }

    private Specification<RagRetrievalLog> retrievalModeEquals(String retrievalMode) {
        if (retrievalMode == null) {
            return null;
        }
        return (root, query, builder) -> builder.equal(root.get("retrievalMode"), retrievalMode);
    }

    private Specification<RagRetrievalLog> businessDomainEquals(String businessDomain) {
        if (businessDomain == null) {
            return null;
        }
        return (root, query, builder) -> builder.equal(root.get("businessDomain"), businessDomain);
    }

    private Specification<RagRetrievalLog> documentTypeEquals(String documentType) {
        if (documentType == null) {
            return null;
        }
        return (root, query, builder) -> builder.equal(root.get("documentType"), documentType);
    }

    private Specification<RagRetrievalLog> permissionLevelEquals(String permissionLevel) {
        if (permissionLevel == null) {
            return null;
        }
        return (root, query, builder) -> builder.equal(root.get("permissionLevel"), permissionLevel);
    }

    private Specification<RagRetrievalLog> statusEquals(RagRetrievalStatus status) {
        if (status == null) {
            return null;
        }
        return (root, query, builder) -> builder.equal(root.get("status"), status);
    }

    private Specification<RagRetrievalLog> conversationIdEquals(UUID conversationId) {
        if (conversationId == null) {
            return null;
        }
        return (root, query, builder) -> builder.equal(root.get("conversationId"), conversationId);
    }

    private Specification<RagRetrievalLog> messageIdEquals(UUID messageId) {
        if (messageId == null) {
            return null;
        }
        return (root, query, builder) -> builder.equal(root.get("messageId"), messageId);
    }

    private Specification<RagRetrievalLog> workflowRunIdEquals(UUID workflowRunId) {
        if (workflowRunId == null) {
            return null;
        }
        return (root, query, builder) -> builder.equal(root.get("workflowRunId"), workflowRunId);
    }

    private Specification<RagRetrievalLog> workflowStepIdEquals(UUID workflowStepId) {
        if (workflowStepId == null) {
            return null;
        }
        return (root, query, builder) -> builder.equal(root.get("workflowStepId"), workflowStepId);
    }

    private Specification<RagRetrievalLog> traceIdEquals(String traceId) {
        if (traceId == null) {
            return null;
        }
        return (root, query, builder) -> builder.equal(root.get("traceId"), traceId);
    }

    private Specification<RagRetrievalLog> createdAtGreaterThanOrEqualTo(Instant createdFrom) {
        if (createdFrom == null) {
            return null;
        }
        return (root, query, builder) -> builder.greaterThanOrEqualTo(root.get("createdAt"), createdFrom);
    }

    private Specification<RagRetrievalLog> createdAtLessThanOrEqualTo(Instant createdTo) {
        if (createdTo == null) {
            return null;
        }
        return (root, query, builder) -> builder.lessThanOrEqualTo(root.get("createdAt"), createdTo);
    }

    private Specification<RagRetrievalLog> and(
            Specification<RagRetrievalLog> left,
            Specification<RagRetrievalLog> right) {
        if (right == null) {
            return left;
        }
        return left.and(right);
    }

    private Specification<RagRetrievalLog> unrestricted() {
        return (root, query, builder) -> builder.conjunction();
    }
}
