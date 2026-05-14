package com.agentweave.tool.repository;

import com.agentweave.tool.domain.ToolDefinitionEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ToolDefinitionRepository extends JpaRepository<ToolDefinitionEntity, UUID> {

    Optional<ToolDefinitionEntity> findByCode(String code);

    Optional<ToolDefinitionEntity> findByPermissionCode(String permissionCode);

    List<ToolDefinitionEntity> findAllByOrderByCodeAsc();
}
