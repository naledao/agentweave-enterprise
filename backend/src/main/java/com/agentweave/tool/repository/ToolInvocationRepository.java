package com.agentweave.tool.repository;

import com.agentweave.tool.domain.ToolInvocationEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ToolInvocationRepository
        extends JpaRepository<ToolInvocationEntity, UUID>, JpaSpecificationExecutor<ToolInvocationEntity> {

    Optional<ToolInvocationEntity> findFirstByTraceIdOrderByCreatedAtDesc(String traceId);
}
