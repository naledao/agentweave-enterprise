package com.agentweave.conversation.repository;

import com.agentweave.conversation.domain.ConversationMessageEntity;
import com.agentweave.conversation.domain.MessageRole;
import com.agentweave.conversation.domain.MessageStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConversationMessageRepository extends JpaRepository<ConversationMessageEntity, UUID> {

    Optional<ConversationMessageEntity> findByIdAndConversation_Id(UUID id, UUID conversationId);

    @Query(
            value = """
                    SELECT *
                    FROM conversation_messages
                    WHERE conversation_id = :conversationId
                    ORDER BY
                        created_at ASC,
                        CASE role
                            WHEN 'USER' THEN 0
                            WHEN 'ASSISTANT' THEN 1
                            WHEN 'TOOL' THEN 2
                            ELSE 3
                        END ASC
                    """,
            countQuery = """
                    SELECT COUNT(*)
                    FROM conversation_messages
                    WHERE conversation_id = :conversationId
                    """,
            nativeQuery = true)
    Page<ConversationMessageEntity> findConversationMessagesAsc(
            @Param("conversationId") UUID conversationId,
            Pageable pageable);

    Optional<ConversationMessageEntity> findFirstByConversation_IdAndRoleAndStatusOrderByCreatedAtDesc(
            UUID conversationId,
            MessageRole role,
            MessageStatus status);

    @Query(
            value = """
                    SELECT *
                    FROM conversation_messages
                    WHERE role = 'ASSISTANT'
                      AND user_id = :userId
                      AND metadata LIKE CONCAT('%', :documentId, '%')
                    ORDER BY created_at DESC
                    LIMIT :limit
                    """,
            nativeQuery = true)
    List<ConversationMessageEntity> findRecentAssistantMessagesReferencingDocument(
            @Param("userId") UUID userId,
            @Param("documentId") String documentId,
            @Param("limit") int limit);
}
