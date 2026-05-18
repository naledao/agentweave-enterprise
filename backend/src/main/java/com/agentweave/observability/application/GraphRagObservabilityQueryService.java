package com.agentweave.observability.application;

import com.agentweave.conversation.domain.ConversationEntity;
import com.agentweave.graphrag.domain.GraphRagIndexLog;
import com.agentweave.graphrag.domain.GraphRagIndexStatus;
import com.agentweave.graphrag.domain.GraphRagRetrievalLog;
import com.agentweave.graphrag.domain.GraphRagRetrievalStatus;
import com.agentweave.graphrag.repository.GraphRagIndexLogRepository;
import com.agentweave.graphrag.repository.GraphRagRetrievalLogRepository;
import com.agentweave.knowledge.domain.DocumentEntity;
import com.agentweave.observability.dto.GraphRagIndexLogListResponse;
import com.agentweave.observability.dto.GraphRagIndexLogQueryRequest;
import com.agentweave.observability.dto.GraphRagIndexLogResponse;
import com.agentweave.observability.dto.GraphRagRetrievalLogListResponse;
import com.agentweave.observability.dto.GraphRagRetrievalLogQueryRequest;
import com.agentweave.observability.dto.GraphRagRetrievalLogResponse;
import com.agentweave.observability.dto.GraphRagSummaryResponse;
import com.agentweave.shared.security.CurrentUser;
import com.agentweave.shared.security.CurrentUserService;
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
public class GraphRagObservabilityQueryService {

    private static final String OBSERVABILITY_READ = "observability:read";

    private final GraphRagIndexLogRepository graphRagIndexLogRepository;
    private final GraphRagRetrievalLogRepository graphRagRetrievalLogRepository;
    private final CurrentUserService currentUserService;

    public GraphRagObservabilityQueryService(
            GraphRagIndexLogRepository graphRagIndexLogRepository,
            GraphRagRetrievalLogRepository graphRagRetrievalLogRepository,
            CurrentUserService currentUserService) {
        this.graphRagIndexLogRepository = graphRagIndexLogRepository;
        this.graphRagRetrievalLogRepository = graphRagRetrievalLogRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public GraphRagSummaryResponse summary() {
        CurrentUser currentUser = currentUserService.requireCurrentUser();
        GraphRagIndexLogResponse latestIndexLog = graphRagIndexLogRepository
                .findAll(indexReadableBy(currentUser), latestOne())
                .stream()
                .findFirst()
                .map(GraphRagIndexLogResponse::from)
                .orElse(null);
        GraphRagRetrievalLogResponse latestRetrievalLog = graphRagRetrievalLogRepository
                .findAll(retrievalReadableBy(currentUser), latestOne())
                .stream()
                .findFirst()
                .map(GraphRagRetrievalLogResponse::from)
                .orElse(null);
        return new GraphRagSummaryResponse(
                latestIndexLog,
                latestRetrievalLog,
                graphRagIndexLogRepository.count(indexReadableBy(currentUser)),
                graphRagRetrievalLogRepository.count(retrievalReadableBy(currentUser)));
    }

    @Transactional(readOnly = true)
    public GraphRagIndexLogListResponse listIndexLogs(GraphRagIndexLogQueryRequest request) {
        CurrentUser currentUser = currentUserService.requireCurrentUser();
        Pageable pageable = PageRequest.of(
                request.pageNumber(),
                request.pageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<GraphRagIndexLogResponse> logs = graphRagIndexLogRepository
                .findAll(indexQuerySpec(request, currentUser), pageable)
                .map(GraphRagIndexLogResponse::from);
        return GraphRagIndexLogListResponse.from(logs);
    }

    @Transactional(readOnly = true)
    public GraphRagRetrievalLogListResponse listRetrievalLogs(GraphRagRetrievalLogQueryRequest request) {
        CurrentUser currentUser = currentUserService.requireCurrentUser();
        Pageable pageable = PageRequest.of(
                request.pageNumber(),
                request.pageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<GraphRagRetrievalLogResponse> logs = graphRagRetrievalLogRepository
                .findAll(retrievalQuerySpec(request, currentUser), pageable)
                .map(GraphRagRetrievalLogResponse::from);
        return GraphRagRetrievalLogListResponse.from(logs);
    }

    private Specification<GraphRagIndexLog> indexQuerySpec(
            GraphRagIndexLogQueryRequest request,
            CurrentUser currentUser) {
        Specification<GraphRagIndexLog> spec = indexReadableBy(currentUser);
        spec = andIndex(spec, indexDocumentIdEquals(request.documentId()));
        spec = andIndex(spec, indexTraceIdEquals(request.normalizedTraceId()));
        spec = andIndex(spec, indexStatusEquals(request.status()));
        spec = andIndex(spec, neo4jEnabledEquals(request.neo4jEnabled()));
        spec = andIndex(spec, indexCreatedAtGreaterThanOrEqualTo(request.createdFrom()));
        return andIndex(spec, indexCreatedAtLessThanOrEqualTo(request.createdTo()));
    }

    private Specification<GraphRagRetrievalLog> retrievalQuerySpec(
            GraphRagRetrievalLogQueryRequest request,
            CurrentUser currentUser) {
        Specification<GraphRagRetrievalLog> spec = retrievalReadableBy(currentUser);
        spec = andRetrieval(spec, retrievalModeEquals(request.normalizedRetrievalMode()));
        spec = andRetrieval(spec, businessDomainEquals(request.normalizedBusinessDomain()));
        spec = andRetrieval(spec, permissionLevelEquals(request.normalizedPermissionLevel()));
        spec = andRetrieval(spec, retrievalStatusEquals(request.status()));
        spec = andRetrieval(spec, conversationIdEquals(request.conversationId()));
        spec = andRetrieval(spec, messageIdEquals(request.messageId()));
        spec = andRetrieval(spec, workflowRunIdEquals(request.workflowRunId()));
        spec = andRetrieval(spec, workflowStepIdEquals(request.workflowStepId()));
        spec = andRetrieval(spec, retrievalDocumentIdEquals(request.documentId()));
        spec = andRetrieval(spec, retrievalTraceIdEquals(request.normalizedTraceId()));
        spec = andRetrieval(spec, retrievalCreatedAtGreaterThanOrEqualTo(request.createdFrom()));
        return andRetrieval(spec, retrievalCreatedAtLessThanOrEqualTo(request.createdTo()));
    }

    private Specification<GraphRagIndexLog> indexReadableBy(CurrentUser user) {
        if (canReadAll(user)) {
            return unrestrictedIndex();
        }
        UUID userId = user.id();
        return (root, query, builder) -> {
            Join<GraphRagIndexLog, DocumentEntity> document = root.join("document", JoinType.LEFT);
            return builder.equal(document.get("uploadedBy"), userId);
        };
    }

    private Specification<GraphRagRetrievalLog> retrievalReadableBy(CurrentUser user) {
        if (canReadAll(user)) {
            return unrestrictedRetrieval();
        }
        UUID userId = user.id();
        return (root, query, builder) -> {
            Join<GraphRagRetrievalLog, ConversationEntity> conversation = root.join("conversation", JoinType.LEFT);
            return builder.equal(conversation.get("ownerUserId"), userId);
        };
    }

    private boolean canReadAll(CurrentUser user) {
        return user.hasRole("ADMIN") || user.hasPermission(OBSERVABILITY_READ);
    }

    private Specification<GraphRagIndexLog> indexDocumentIdEquals(UUID documentId) {
        if (documentId == null) {
            return null;
        }
        return (root, query, builder) -> builder.equal(root.get("documentId"), documentId);
    }

    private Specification<GraphRagIndexLog> indexTraceIdEquals(String traceId) {
        if (traceId == null) {
            return null;
        }
        return (root, query, builder) -> builder.equal(root.get("traceId"), traceId);
    }

    private Specification<GraphRagIndexLog> indexStatusEquals(GraphRagIndexStatus status) {
        if (status == null) {
            return null;
        }
        return (root, query, builder) -> builder.equal(root.get("status"), status);
    }

    private Specification<GraphRagIndexLog> neo4jEnabledEquals(Boolean neo4jEnabled) {
        if (neo4jEnabled == null) {
            return null;
        }
        return (root, query, builder) -> builder.equal(root.get("neo4jEnabled"), neo4jEnabled);
    }

    private Specification<GraphRagIndexLog> indexCreatedAtGreaterThanOrEqualTo(Instant createdFrom) {
        if (createdFrom == null) {
            return null;
        }
        return (root, query, builder) -> builder.greaterThanOrEqualTo(root.get("createdAt"), createdFrom);
    }

    private Specification<GraphRagIndexLog> indexCreatedAtLessThanOrEqualTo(Instant createdTo) {
        if (createdTo == null) {
            return null;
        }
        return (root, query, builder) -> builder.lessThanOrEqualTo(root.get("createdAt"), createdTo);
    }

    private Specification<GraphRagRetrievalLog> retrievalModeEquals(String retrievalMode) {
        if (retrievalMode == null) {
            return null;
        }
        return (root, query, builder) -> builder.equal(root.get("retrievalMode"), retrievalMode);
    }

    private Specification<GraphRagRetrievalLog> businessDomainEquals(String businessDomain) {
        if (businessDomain == null) {
            return null;
        }
        return (root, query, builder) -> builder.equal(root.get("businessDomain"), businessDomain);
    }

    private Specification<GraphRagRetrievalLog> permissionLevelEquals(String permissionLevel) {
        if (permissionLevel == null) {
            return null;
        }
        return (root, query, builder) -> builder.equal(root.get("permissionLevel"), permissionLevel);
    }

    private Specification<GraphRagRetrievalLog> retrievalStatusEquals(GraphRagRetrievalStatus status) {
        if (status == null) {
            return null;
        }
        return (root, query, builder) -> builder.equal(root.get("status"), status);
    }

    private Specification<GraphRagRetrievalLog> conversationIdEquals(UUID conversationId) {
        if (conversationId == null) {
            return null;
        }
        return (root, query, builder) -> builder.equal(root.get("conversationId"), conversationId);
    }

    private Specification<GraphRagRetrievalLog> messageIdEquals(UUID messageId) {
        if (messageId == null) {
            return null;
        }
        return (root, query, builder) -> builder.equal(root.get("messageId"), messageId);
    }

    private Specification<GraphRagRetrievalLog> workflowRunIdEquals(UUID workflowRunId) {
        if (workflowRunId == null) {
            return null;
        }
        return (root, query, builder) -> builder.equal(root.get("workflowRunId"), workflowRunId);
    }

    private Specification<GraphRagRetrievalLog> workflowStepIdEquals(UUID workflowStepId) {
        if (workflowStepId == null) {
            return null;
        }
        return (root, query, builder) -> builder.equal(root.get("workflowStepId"), workflowStepId);
    }

    private Specification<GraphRagRetrievalLog> retrievalDocumentIdEquals(UUID documentId) {
        if (documentId == null) {
            return null;
        }
        return (root, query, builder) -> builder.equal(root.get("documentId"), documentId);
    }

    private Specification<GraphRagRetrievalLog> retrievalTraceIdEquals(String traceId) {
        if (traceId == null) {
            return null;
        }
        return (root, query, builder) -> builder.equal(root.get("traceId"), traceId);
    }

    private Specification<GraphRagRetrievalLog> retrievalCreatedAtGreaterThanOrEqualTo(Instant createdFrom) {
        if (createdFrom == null) {
            return null;
        }
        return (root, query, builder) -> builder.greaterThanOrEqualTo(root.get("createdAt"), createdFrom);
    }

    private Specification<GraphRagRetrievalLog> retrievalCreatedAtLessThanOrEqualTo(Instant createdTo) {
        if (createdTo == null) {
            return null;
        }
        return (root, query, builder) -> builder.lessThanOrEqualTo(root.get("createdAt"), createdTo);
    }

    private Specification<GraphRagIndexLog> andIndex(
            Specification<GraphRagIndexLog> left,
            Specification<GraphRagIndexLog> right) {
        if (right == null) {
            return left;
        }
        return left.and(right);
    }

    private Specification<GraphRagRetrievalLog> andRetrieval(
            Specification<GraphRagRetrievalLog> left,
            Specification<GraphRagRetrievalLog> right) {
        if (right == null) {
            return left;
        }
        return left.and(right);
    }

    private Specification<GraphRagIndexLog> unrestrictedIndex() {
        return (root, query, builder) -> builder.conjunction();
    }

    private Specification<GraphRagRetrievalLog> unrestrictedRetrieval() {
        return (root, query, builder) -> builder.conjunction();
    }

    private Pageable latestOne() {
        return PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "createdAt"));
    }
}
