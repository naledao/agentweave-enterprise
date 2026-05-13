package com.agentweave.conversation.repository;

import com.agentweave.conversation.domain.ConversationEntity;
import com.agentweave.conversation.domain.ConversationStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConversationRepository extends JpaRepository<ConversationEntity, UUID> {

    @Query("""
            SELECT c
            FROM ConversationEntity c
            WHERE c.ownerUserId = :ownerUserId
              AND c.status <> :excludedStatus
            """)
    Page<ConversationEntity> findOwnedConversations(
            @Param("ownerUserId") UUID ownerUserId,
            @Param("excludedStatus") ConversationStatus excludedStatus,
            Pageable pageable);

    @Query("""
            SELECT c
            FROM ConversationEntity c
            WHERE c.ownerUserId = :ownerUserId
              AND c.status <> :excludedStatus
              AND LOWER(c.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
            """)
    Page<ConversationEntity> searchOwnedConversations(
            @Param("ownerUserId") UUID ownerUserId,
            @Param("excludedStatus") ConversationStatus excludedStatus,
            @Param("keyword") String keyword,
            Pageable pageable);

    @EntityGraph(attributePaths = "messages")
    Optional<ConversationEntity> findWithMessagesByIdAndOwnerUserId(UUID id, UUID ownerUserId);

    Optional<ConversationEntity> findByIdAndOwnerUserId(UUID id, UUID ownerUserId);
}
