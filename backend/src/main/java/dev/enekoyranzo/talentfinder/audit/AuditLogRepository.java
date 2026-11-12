package dev.enekoyranzo.talentfinder.audit;

import org.springframework.data.repository.Repository;

/**
 * Las consultas de lectura irán siempre acotadas por organización.
 */
public interface AuditLogRepository extends Repository<AuditLog, Long> {

    AuditLog save(AuditLog entry);
}