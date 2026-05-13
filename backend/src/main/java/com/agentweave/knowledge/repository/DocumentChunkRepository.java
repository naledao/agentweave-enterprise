package com.agentweave.knowledge.repository;

import com.agentweave.knowledge.domain.DocumentChunkEntity;
import com.agentweave.knowledge.domain.DocumentChunkStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentChunkRepository extends JpaRepository<DocumentChunkEntity, UUID> {

    List<DocumentChunkEntity> findByDocumentIdOrderByChunkIndexAsc(UUID documentId);

    List<DocumentChunkEntity> findByDocumentIdAndStatusOrderByChunkIndexAsc(
            UUID documentId,
            DocumentChunkStatus status);

    long countByDocumentId(UUID documentId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from DocumentChunkEntity chunk where chunk.documentId = :documentId")
    int deleteByDocumentId(@Param("documentId") UUID documentId);
}
