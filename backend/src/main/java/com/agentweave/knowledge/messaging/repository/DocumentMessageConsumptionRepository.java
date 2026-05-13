package com.agentweave.knowledge.messaging.repository;

import com.agentweave.knowledge.messaging.domain.DocumentMessageConsumptionEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentMessageConsumptionRepository
        extends JpaRepository<DocumentMessageConsumptionEntity, UUID> {
}
