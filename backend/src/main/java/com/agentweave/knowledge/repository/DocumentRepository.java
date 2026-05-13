package com.agentweave.knowledge.repository;

import com.agentweave.knowledge.domain.DocumentEntity;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentRepository extends JpaRepository<DocumentEntity, UUID> {

    Page<DocumentEntity> findByFilenameContainingIgnoreCase(String filename, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<DocumentEntity> findWithLockById(UUID id);
}
