package com.agentweave.workflow.application;

import com.agentweave.workflow.domain.AgentRunEntity;
import com.agentweave.workflow.domain.AgentStepEntity;
import com.agentweave.workflow.domain.AgentStepType;
import com.agentweave.workflow.dto.WorkflowReviewResult;
import com.agentweave.workflow.repository.AgentStepRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AgentStepService {

    private final AgentStepRepository agentStepRepository;

    public AgentStepService(AgentStepRepository agentStepRepository) {
        this.agentStepRepository = agentStepRepository;
    }

    @Transactional(readOnly = true)
    public List<AgentStepEntity> getStepsByRunId(UUID runId) {
        return agentStepRepository.findByRunIdOrderByStepIndexAsc(runId);
    }

    @Transactional
    public AgentStepEntity createStep(AgentRunEntity run, int stepIndex, AgentStepType stepType, String nodeName) {
        AgentStepEntity step = new AgentStepEntity(
                UUID.randomUUID(),
                run,
                stepIndex,
                stepType,
                nodeName);
        return agentStepRepository.save(step);
    }

    @Transactional
    public void startStep(UUID stepId) {
        AgentStepEntity step = agentStepRepository.findById(stepId)
                .orElseThrow(() -> new IllegalArgumentException("Step not found: " + stepId));
        step.start(Instant.now());
        agentStepRepository.save(step);
    }

    @Transactional
    public void recordInputSummary(UUID stepId, String inputSummary) {
        AgentStepEntity step = agentStepRepository.findById(stepId)
                .orElseThrow(() -> new IllegalArgumentException("Step not found: " + stepId));
        step.setInputSummary(inputSummary);
        agentStepRepository.save(step);
    }

    @Transactional
    public void completeStep(UUID stepId, String outputSummary) {
        completeStep(stepId, outputSummary, List.of(), List.of(), List.of());
    }

    @Transactional
    public void completeStep(
            UUID stepId,
            String outputSummary,
            List<WorkflowReviewResult.Citation> citations,
            List<WorkflowReviewResult.GraphPath> graphPaths,
            List<WorkflowReviewResult.ToolCallResult> toolCalls) {
        AgentStepEntity step = agentStepRepository.findById(stepId)
                .orElseThrow(() -> new IllegalArgumentException("Step not found: " + stepId));
        step.recordArtifacts(citations, graphPaths, toolCalls);
        step.succeed(outputSummary, Instant.now());
        agentStepRepository.save(step);
    }

    @Transactional
    public void failStep(UUID stepId, String errorCode, String errorMessage) {
        AgentStepEntity step = agentStepRepository.findById(stepId)
                .orElseThrow(() -> new IllegalArgumentException("Step not found: " + stepId));
        step.fail(errorCode, errorMessage, Instant.now());
        agentStepRepository.save(step);
    }

    @Transactional
    public void skipStep(UUID stepId) {
        AgentStepEntity step = agentStepRepository.findById(stepId)
                .orElseThrow(() -> new IllegalArgumentException("Step not found: " + stepId));
        step.skip(Instant.now());
        agentStepRepository.save(step);
    }

    @Transactional
    public void recordRetry(UUID runId, int stepIndex, String reason) {
        agentStepRepository.findByRunIdAndStepIndex(runId, stepIndex)
                .ifPresent(step -> {
                    step.recordRetry(reason, Instant.now());
                    agentStepRepository.save(step);
                });
    }

    @Transactional(readOnly = true)
    public AgentStepEntity findByRunIdAndStepIndex(UUID runId, int stepIndex) {
        return agentStepRepository.findByRunIdAndStepIndex(runId, stepIndex)
                .orElseThrow(() -> new IllegalArgumentException("Step not found for run " + runId + " and index " + stepIndex));
    }

    @Transactional(readOnly = true)
    public int nextStepIndex(UUID runId) {
        return agentStepRepository.findFirstByRun_IdOrderByStepIndexDesc(runId)
                .map(step -> step.getStepIndex() + 1)
                .orElse(0);
    }
}
