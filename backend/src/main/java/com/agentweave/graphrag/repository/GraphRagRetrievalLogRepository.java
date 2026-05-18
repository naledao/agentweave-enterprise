package com.agentweave.graphrag.repository;

import com.agentweave.graphrag.domain.GraphRagRetrievalLog;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface GraphRagRetrievalLogRepository
        extends JpaRepository<GraphRagRetrievalLog, UUID>, JpaSpecificationExecutor<GraphRagRetrievalLog> {

    Optional<GraphRagRetrievalLog> findFirstByConversationIdOrderByCreatedAtDesc(UUID conversationId);

    Optional<GraphRagRetrievalLog> findFirstByTraceIdOrderByCreatedAtDesc(String traceId);
}
