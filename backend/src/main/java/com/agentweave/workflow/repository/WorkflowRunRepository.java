package com.agentweave.workflow.repository;

import com.agentweave.workflow.domain.AgentRunEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkflowRunRepository extends JpaRepository<AgentRunEntity, UUID> {
}
