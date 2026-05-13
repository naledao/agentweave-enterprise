package com.agentweave.shared.audit;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLogEntity, UUID> {

    long countByEventTypeAndUsername(AuditEventType eventType, String username);

    List<AuditLogEntity> findByEventTypeAndUsernameOrderByCreatedAtDesc(
            AuditEventType eventType,
            String username);
}
