package ar.edu.itba.certiflow.models.audit;

import ar.edu.itba.certiflow.models.shared.Person;
import java.time.Instant;
import java.util.Objects;

/** Coordinates recording audit facts without coupling decorators to the storage port. */
public final class AuditTrail {

    private final AuditService auditService;

    public AuditTrail(AuditService auditService) {
        this.auditService = Objects.requireNonNull(auditService, "service");
    }

    public void record(Object subject, AuditAction auditAction, Person performedBy, Instant occurredAt, String detail) {
        auditService.record(new AuditEntry(subject, auditAction, performedBy, occurredAt, detail));
    }
}
