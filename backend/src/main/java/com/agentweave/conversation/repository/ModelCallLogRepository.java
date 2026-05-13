package com.agentweave.conversation.repository;

import com.agentweave.conversation.domain.ModelCallLogEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ModelCallLogRepository extends JpaRepository<ModelCallLogEntity, UUID> {

    long countByConversationId(UUID conversationId);

    Optional<ModelCallLogEntity> findFirstByConversationIdOrderByCreatedAtDesc(UUID conversationId);
}
