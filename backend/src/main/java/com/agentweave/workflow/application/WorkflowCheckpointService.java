package com.agentweave.workflow.application;

import com.agentweave.workflow.domain.AgentRunEntity;
import com.agentweave.workflow.domain.WorkflowCheckpointEntity;
import com.agentweave.shared.exception.BusinessException;
import com.agentweave.shared.exception.ErrorCode;
import com.agentweave.workflow.repository.WorkflowCheckpointRepository;
import com.agentweave.workflow.state.AgentWorkflowState;
import com.agentweave.workflow.state.AgentWorkflowStateSerializer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkflowCheckpointService {

    public static final int STATE_VERSION = 1;

    private final WorkflowCheckpointRepository workflowCheckpointRepository;
    private final WorkflowRunService workflowRunService;
    private final AgentWorkflowStateSerializer stateSerializer;

    public WorkflowCheckpointService(
            WorkflowCheckpointRepository workflowCheckpointRepository,
            WorkflowRunService workflowRunService,
            AgentWorkflowStateSerializer stateSerializer) {
        this.workflowCheckpointRepository = workflowCheckpointRepository;
        this.workflowRunService = workflowRunService;
        this.stateSerializer = stateSerializer;
    }

    @Transactional
    public WorkflowCheckpointEntity save(String nodeName, AgentWorkflowState state) {
        AgentRunEntity run = workflowRunService.getEntityById(state.runId());
        String payload = stateSerializer.writePayload(state);
        WorkflowCheckpointEntity checkpoint = new WorkflowCheckpointEntity(
                UUID.randomUUID(),
                run,
                state.currentStepIndex(),
                nodeName,
                STATE_VERSION,
                payload,
                checksum(payload));
        return workflowCheckpointRepository.save(checkpoint);
    }

    @Transactional(readOnly = true)
    public Optional<AgentWorkflowState> latestState(UUID runId) {
        return workflowCheckpointRepository.findFirstByRun_IdOrderByCreatedAtDesc(runId)
                .map(this::toState);
    }

    @Transactional(readOnly = true)
    public Optional<WorkflowCheckpointEntity> latestCheckpoint(UUID runId) {
        return workflowCheckpointRepository.findFirstByRun_IdOrderByCreatedAtDesc(runId);
    }

    public AgentWorkflowState toState(WorkflowCheckpointEntity checkpoint) {
        if (checkpoint.getStateVersion() != STATE_VERSION) {
            throw new BusinessException(
                    ErrorCode.WORKFLOW_CHECKPOINT_INVALID,
                    "Unsupported workflow state version: " + checkpoint.getStateVersion());
        }
        if (!checksum(checkpoint.getStatePayload()).equals(checkpoint.getChecksum())) {
            throw new BusinessException(
                    ErrorCode.WORKFLOW_CHECKPOINT_INVALID,
                    "Workflow checkpoint checksum mismatch: " + checkpoint.getId());
        }
        return stateSerializer.readPayload(checkpoint.getStatePayload());
    }

    private String checksum(String payload) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }
}
