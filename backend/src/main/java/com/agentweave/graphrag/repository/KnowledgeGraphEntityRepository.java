package com.agentweave.graphrag.repository;

import com.agentweave.graphrag.domain.KnowledgeGraphEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface KnowledgeGraphEntityRepository extends JpaRepository<KnowledgeGraphEntity, UUID> {

    List<KnowledgeGraphEntity> findBySourceDocumentIdOrderByNormalizedNameAsc(UUID sourceDocumentId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from KnowledgeGraphEntity entity where entity.sourceDocumentId = :sourceDocumentId")
    int deleteBySourceDocumentId(@Param("sourceDocumentId") UUID sourceDocumentId);
}
