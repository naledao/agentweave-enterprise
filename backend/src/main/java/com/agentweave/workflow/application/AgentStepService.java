package com.agentweave.workflow.application;

import com.agentweave.workflow.domain.AgentStepEntity;
import com.agentweave.workflow.domain.AgentStepType;
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
    public AgentStepEntity createStep(UUID runId, int stepIndex, AgentStepType stepType, String nodeName) {
        AgentStepEntity step = new AgentStepEntity(
                UUID.randomUUID(),
                null,
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
    public void completeStep(UUID stepId, String outputSummary) {
        AgentStepEntity step = agentStepRepository.findById(stepId)
                .orElseThrow(() -> new IllegalArgumentException("Step not found: " + stepId));
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
}
