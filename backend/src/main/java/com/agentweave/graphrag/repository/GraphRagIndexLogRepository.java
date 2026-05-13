package com.agentweave.graphrag.repository;

import com.agentweave.graphrag.domain.GraphRagIndexLog;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GraphRagIndexLogRepository extends JpaRepository<GraphRagIndexLog, UUID> {

    Optional<GraphRagIndexLog> findFirstByDocumentIdOrderByCreatedAtDesc(UUID documentId);
}
