package com.agentweave.workflow.application;

import com.agentweave.shared.exception.ResourceNotFoundException;
import com.agentweave.shared.security.CurrentUser;
import com.agentweave.shared.security.CurrentUserService;
import com.agentweave.shared.tracing.TraceIdProvider;
import com.agentweave.workflow.domain.AgentRunEntity;
import com.agentweave.workflow.domain.AgentStepEntity;
import com.agentweave.workflow.domain.AgentStepStatus;
import com.agentweave.workflow.domain.AgentStepType;
import com.agentweave.workflow.domain.WorkflowRunStatus;
import com.agentweave.workflow.domain.WorkflowStatusMachine;
import com.agentweave.workflow.dto.CreateWorkflowRunRequest;
import com.agentweave.workflow.dto.WorkflowRunListItemResponse;
import com.agentweave.workflow.dto.WorkflowRunListResponse;
import com.agentweave.workflow.dto.WorkflowRunQueryRequest;
import com.agentweave.workflow.dto.WorkflowRunResponse;
import com.agentweave.workflow.dto.WorkflowStepResponse;
import com.agentweave.workflow.repository.AgentStepRepository;
import com.agentweave.workflow.repository.WorkflowRunRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkflowRunService {

    private final WorkflowRunRepository workflowRunRepository;
    private final AgentStepRepository agentStepRepository;
    private final CurrentUserService currentUserService;
    private final TraceIdProvider traceIdProvider;

    public WorkflowRunService(
            WorkflowRunRepository workflowRunRepository,
            AgentStepRepository agentStepRepository,
            CurrentUserService currentUserService,
            TraceIdProvider traceIdProvider) {
        this.workflowRunRepository = workflowRunRepository;
        this.agentStepRepository = agentStepRepository;
        this.currentUserService = currentUserService;
        this.traceIdProvider = traceIdProvider;
    }

    @Transactional
    public WorkflowRunResponse create(CreateWorkflowRunRequest request) {
        CurrentUser user = currentUserService.requireCurrentUser();

        AgentRunEntity run = new AgentRunEntity(
                UUID.randomUUID(),
                user.id(),
                request.goal());

        run.setConversationId(request.conversationId());
        run.setTraceId(traceIdProvider.currentTraceId());

        AgentRunEntity saved = workflowRunRepository.save(run);
        return WorkflowRunResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public WorkflowRunListResponse list(WorkflowRunQueryRequest request) {
        CurrentUser user = currentUserService.requireCurrentUser();
        Pageable pageable = PageRequest.of(
                request.pageNumber(),
                request.pageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<WorkflowRunListItemResponse> runs = workflowRunRepository
                .findAll(querySpec(request, user), pageable)
                .map(WorkflowRunListItemResponse::from);
        return WorkflowRunListResponse.from(runs);
    }

    @Transactional(readOnly = true)
    public WorkflowRunResponse get(UUID runId) {
        CurrentUser user = currentUserService.requireCurrentUser();
        AgentRunEntity run = require(runId);
        validateAccess(user, run);
        return WorkflowRunResponse.from(run);
    }

    @Transactional(readOnly = true)
    public List<WorkflowStepResponse> getSteps(UUID runId) {
        CurrentUser user = currentUserService.requireCurrentUser();
        AgentRunEntity run = require(runId);
        validateAccess(user, run);

        List<AgentStepEntity> steps = agentStepRepository.findByRunIdOrderByStepIndexAsc(runId);
        return steps.stream()
                .map(WorkflowStepResponse::from)
                .toList();
    }

    @Transactional
    public WorkflowRunResponse cancel(UUID runId) {
        CurrentUser user = currentUserService.requireCurrentUser();
        AgentRunEntity run = require(runId);
        validateAccess(user, run);

        WorkflowStatusMachine.validateTransition(run.getStatus(), WorkflowRunStatus.CANCELLED);
        run.cancel(Instant.now());

        AgentRunEntity saved = workflowRunRepository.save(run);
        return WorkflowRunResponse.from(saved);
    }

    @Transactional
    public void transitionTo(UUID runId, WorkflowRunStatus targetStatus) {
        AgentRunEntity run = require(runId);
        WorkflowStatusMachine.validateTransition(run.getStatus(), targetStatus);

        switch (targetStatus) {
            case PLANNING -> run.startPlanning(Instant.now());
            case EXECUTING -> run.startExecuting(Instant.now());
            case WAITING_APPROVAL -> run.waitForApproval();
            case REVIEWING -> run.startReviewing(Instant.now());
            default -> throw new IllegalStateException("Use dedicated methods for terminal states");
        }

        workflowRunRepository.save(run);
    }

    @Transactional
    public void markSucceeded(UUID runId, String finalAnswer) {
        AgentRunEntity run = require(runId);
        WorkflowStatusMachine.validateTransition(run.getStatus(), WorkflowRunStatus.SUCCEEDED);
        run.succeed(finalAnswer, Instant.now());
        workflowRunRepository.save(run);
    }

    @Transactional
    public void markFailed(UUID runId, String errorCode, String errorMessage) {
        AgentRunEntity run = require(runId);
        if (run.getStatus() != WorkflowRunStatus.FAILED) {
            WorkflowStatusMachine.validateTransition(run.getStatus(), WorkflowRunStatus.FAILED);
        }
        run.fail(errorCode, errorMessage, Instant.now());
        workflowRunRepository.save(run);
    }

    @Transactional
    public AgentStepEntity addStep(UUID runId, AgentStepType stepType, String nodeName) {
        AgentRunEntity run = require(runId);
        int stepIndex = run.getCurrentStepIndex();

        AgentStepEntity step = new AgentStepEntity(
                UUID.randomUUID(),
                run,
                stepIndex,
                stepType,
                nodeName);

        AgentStepEntity saved = agentStepRepository.save(step);
        run.advanceStep();
        workflowRunRepository.save(run);

        return saved;
    }

    @Transactional
    public void startStep(UUID stepId) {
        AgentStepEntity step = requireStep(stepId);
        step.start(Instant.now());
        agentStepRepository.save(step);
    }

    @Transactional
    public void completeStep(UUID stepId, String outputSummary) {
        AgentStepEntity step = requireStep(stepId);
        step.succeed(outputSummary, Instant.now());
        agentStepRepository.save(step);
    }

    @Transactional
    public void failStep(UUID stepId, String errorCode, String errorMessage) {
        AgentStepEntity step = requireStep(stepId);
        step.fail(errorCode, errorMessage, Instant.now());
        agentStepRepository.save(step);
    }

    @Transactional
    public void updateCurrentStepIndex(UUID runId, int currentStepIndex) {
        AgentRunEntity run = require(runId);
        run.setCurrentStepIndex(currentStepIndex);
        workflowRunRepository.save(run);
    }

    private AgentRunEntity require(UUID runId) {
        return workflowRunRepository.findById(runId)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow run not found: " + runId));
    }

    @Transactional(readOnly = true)
    public AgentRunEntity getEntityById(UUID runId) {
        return require(runId);
    }

    @Transactional(readOnly = true)
    public AgentRunEntity getAccessibleEntityById(UUID runId) {
        CurrentUser user = currentUserService.requireCurrentUser();
        AgentRunEntity run = require(runId);
        validateAccess(user, run);
        return run;
    }

    @Transactional
    public void prepareForRecovery(
            UUID runId,
            WorkflowRunStatus status,
            int currentStepIndex,
            boolean clearFinalAnswer) {
        AgentRunEntity run = require(runId);
        run.prepareRecovery(status, currentStepIndex, clearFinalAnswer, Instant.now());
        workflowRunRepository.save(run);
    }

    @Transactional(readOnly = true)
    public WorkflowRunResponse getInternal(UUID runId) {
        return WorkflowRunResponse.from(require(runId));
    }

    private Specification<AgentRunEntity> querySpec(WorkflowRunQueryRequest request, CurrentUser user) {
        Specification<AgentRunEntity> spec = readableBy(user);
        WorkflowRunStatus status = request.normalizedStatus();
        if (status == null) {
            return spec;
        }
        return spec.and((root, query, builder) -> builder.equal(root.get("status"), status));
    }

    private Specification<AgentRunEntity> readableBy(CurrentUser user) {
        if (user.hasRole("ADMIN")) {
            return (root, query, builder) -> builder.conjunction();
        }
        return (root, query, builder) -> builder.equal(root.get("userId"), user.id());
    }

    private AgentStepEntity requireStep(UUID stepId) {
        return agentStepRepository.findById(stepId)
                .orElseThrow(() -> new ResourceNotFoundException("Agent step not found: " + stepId));
    }

    private void validateAccess(CurrentUser user, AgentRunEntity run) {
        if (!user.hasRole("ADMIN") && !user.id().equals(run.getUserId())) {
            throw new ResourceNotFoundException("Workflow run not found: " + run.getId());
        }
    }
}
