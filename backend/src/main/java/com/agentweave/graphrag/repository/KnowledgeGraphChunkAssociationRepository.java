package com.agentweave.graphrag.repository;

import com.agentweave.graphrag.domain.KnowledgeGraphChunkAssociation;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface KnowledgeGraphChunkAssociationRepository
        extends JpaRepository<KnowledgeGraphChunkAssociation, UUID> {

    List<KnowledgeGraphChunkAssociation> findBySourceDocumentIdOrderByChunkIdAsc(UUID sourceDocumentId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from KnowledgeGraphChunkAssociation association where association.sourceDocumentId = :sourceDocumentId")
    int deleteBySourceDocumentId(@Param("sourceDocumentId") UUID sourceDocumentId);
}
