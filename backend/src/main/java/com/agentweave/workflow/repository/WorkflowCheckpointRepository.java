package com.agentweave.workflow.repository;

import com.agentweave.workflow.domain.WorkflowCheckpointEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkflowCheckpointRepository extends JpaRepository<WorkflowCheckpointEntity, UUID> {

    Optional<WorkflowCheckpointEntity> findFirstByRun_IdOrderByCreatedAtDesc(UUID runId);
}
