-- Registro de eventos de seguridad (RGPD art. 5.2 y 32).
-- Minimización: se guarda user_id, no email ni nombre; no se registra el email de
-- intentos con usuarios inexistentes. La IP es dato personal: hay que purgarla
-- pasado el plazo de retención (job de purga en una fase posterior).
-- Sin clave foránea a users a propósito: el registro debe sobrevivir al borrado de la cuenta,
-- y tras borrarla el user_id deja de apuntar a una persona identificable.
CREATE TABLE audit_log
(
    id              BIGSERIAL PRIMARY KEY,
    occurred_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    organization_id BIGINT,
    user_id         BIGINT,
    action          VARCHAR(60) NOT NULL,
    outcome         VARCHAR(20) NOT NULL,
    ip_address      VARCHAR(45),
    details         TEXT
);

CREATE INDEX idx_audit_org_time ON audit_log (organization_id, occurred_at DESC);
CREATE INDEX idx_audit_time ON audit_log (occurred_at);