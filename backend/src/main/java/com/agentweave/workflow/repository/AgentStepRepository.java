package com.agentweave.workflow.repository;

import com.agentweave.workflow.domain.AgentStepEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AgentStepRepository extends JpaRepository<AgentStepEntity, UUID> {

    List<AgentStepEntity> findByRunIdOrderByStepIndexAsc(UUID runId);
}
