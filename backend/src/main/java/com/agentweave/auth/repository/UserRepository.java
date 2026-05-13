package com.agentweave.auth.repository;

import com.agentweave.auth.domain.UserEntity;
import com.agentweave.auth.domain.UserStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByRoles_IdIn(Collection<UUID> roleIds);

    @EntityGraph(attributePaths = {"roles", "roles.permissions"})
    Optional<UserEntity> findWithRolesByUsername(String username);

    @EntityGraph(attributePaths = {"roles", "roles.permissions"})
    Optional<UserEntity> findWithRolesById(UUID id);

    @EntityGraph(attributePaths = {"roles", "roles.permissions"})
    List<UserEntity> findByIdIn(Collection<UUID> ids);

    @Query("""
            SELECT u
            FROM UserEntity u
            WHERE (:status IS NULL OR u.status = :status)
            """)
    Page<UserEntity> findByStatusFilter(@Param("status") UserStatus status, Pageable pageable);

    @Query("""
            SELECT u
            FROM UserEntity u
            WHERE (:status IS NULL OR u.status = :status)
              AND (
                LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(u.displayName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
            """)
    Page<UserEntity> searchByKeywordAndStatus(
            @Param("keyword") String keyword,
            @Param("status") UserStatus status,
            Pageable pageable);

    @Modifying(flushAutomatically = true)
    @Query(
            value = """
                    UPDATE users
                    SET token_version = token_version + 1
                    WHERE id IN (
                        SELECT user_id
                        FROM user_roles
                        WHERE role_id = :roleId
                    )
                    """,
            nativeQuery = true)
    int incrementTokenVersionByRoleId(@Param("roleId") UUID roleId);
}
