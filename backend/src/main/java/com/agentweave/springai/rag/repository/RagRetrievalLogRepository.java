package com.agentweave.springai.rag.repository;

import com.agentweave.springai.rag.domain.RagRetrievalLog;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface RagRetrievalLogRepository
        extends JpaRepository<RagRetrievalLog, UUID>, JpaSpecificationExecutor<RagRetrievalLog> {

    Optional<RagRetrievalLog> findFirstByConversationIdOrderByCreatedAtDesc(UUID conversationId);

    Optional<RagRetrievalLog> findFirstByMessageIdOrderByCreatedAtDesc(UUID messageId);

    Optional<RagRetrievalLog> findFirstByTraceIdOrderByCreatedAtDesc(String traceId);
}
