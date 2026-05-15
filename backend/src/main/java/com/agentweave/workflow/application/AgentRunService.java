package com.agentweave.workflow.application;

import com.agentweave.workflow.domain.AgentRunEntity;
import com.agentweave.workflow.domain.WorkflowRunStatus;
import com.agentweave.workflow.repository.WorkflowRunRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AgentRunService {

    private final WorkflowRunRepository workflowRunRepository;

    public AgentRunService(WorkflowRunRepository workflowRunRepository) {
        this.workflowRunRepository = workflowRunRepository;
    }

    @Transactional
    public void transitionToPlanning(UUID runId) {
        AgentRunEntity run = workflowRunRepository.findById(runId)
                .orElseThrow(() -> new IllegalArgumentException("Run not found: " + runId));
        run.startPlanning(Instant.now());
        workflowRunRepository.save(run);
    }

    @Transactional
    public void transitionToExecuting(UUID runId) {
        AgentRunEntity run = workflowRunRepository.findById(runId)
                .orElseThrow(() -> new IllegalArgumentException("Run not found: " + runId));
        run.startExecuting(Instant.now());
        workflowRunRepository.save(run);
    }

    @Transactional
    public void transitionToReviewing(UUID runId) {
        AgentRunEntity run = workflowRunRepository.findById(runId)
                .orElseThrow(() -> new IllegalArgumentException("Run not found: " + runId));
        run.startReviewing(Instant.now());
        workflowRunRepository.save(run);
    }

    @Transactional
    public void markSucceeded(UUID runId, String finalAnswer) {
        AgentRunEntity run = workflowRunRepository.findById(runId)
                .orElseThrow(() -> new IllegalArgumentException("Run not found: " + runId));
        run.succeed(finalAnswer, Instant.now());
        workflowRunRepository.save(run);
    }

    @Transactional
    public void markFailed(UUID runId, String errorCode, String errorMessage) {
        AgentRunEntity run = workflowRunRepository.findById(runId)
                .orElseThrow(() -> new IllegalArgumentException("Run not found: " + runId));
        run.fail(errorCode, errorMessage, Instant.now());
        workflowRunRepository.save(run);
    }

    @Transactional
    public void markCancelled(UUID runId) {
        AgentRunEntity run = workflowRunRepository.findById(runId)
                .orElseThrow(() -> new IllegalArgumentException("Run not found: " + runId));
        run.cancel(Instant.now());
        workflowRunRepository.save(run);
    }
}
