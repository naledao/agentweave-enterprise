package com.agentweave.workflow.repository;

import com.agentweave.workflow.domain.WorkflowApprovalEntity;
import com.agentweave.workflow.domain.WorkflowApprovalStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkflowApprovalRepository extends JpaRepository<WorkflowApprovalEntity, UUID> {

    List<WorkflowApprovalEntity> findAllByOrderByCreatedAtDesc();

    List<WorkflowApprovalEntity> findByStatusOrderByCreatedAtDesc(WorkflowApprovalStatus status);

    List<WorkflowApprovalEntity> findByRequestedByOrderByCreatedAtDesc(UUID requestedBy);

    List<WorkflowApprovalEntity> findByRequestedByAndStatusOrderByCreatedAtDesc(
            UUID requestedBy,
            WorkflowApprovalStatus status);

    Optional<WorkflowApprovalEntity> findFirstByRun_IdAndStep_IdOrderByCreatedAtDesc(UUID runId, UUID stepId);
}
