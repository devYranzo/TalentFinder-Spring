package dev.enekoyranzo.talentfinder.audit;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

    private final AuditLogRepository repository;

    public AuditService(AuditLogRepository repository) {
        this.repository = repository;
    }

    /**
     * REQUIRES_NEW: el registro se guarda aunque la operación de negocio haga rollback
     * (por ejemplo, un login fallido).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(Long organizationId, Long userId, String action, String outcome,
                       String ipAddress, String details) {
        repository.save(new AuditLog(organizationId, userId, action, outcome, ipAddress, details));
    }
}