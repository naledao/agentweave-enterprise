package com.agentweave.auth.repository;

import com.agentweave.auth.domain.RoleEntity;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<RoleEntity, UUID> {

    @EntityGraph(attributePaths = "permissions")
    Optional<RoleEntity> findByCode(String code);

    boolean existsByCode(String code);

    @EntityGraph(attributePaths = "permissions")
    List<RoleEntity> findByIdIn(Collection<UUID> ids);
}
