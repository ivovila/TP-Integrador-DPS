package ar.edu.itba.certiflow.domain.audit;

import ar.edu.itba.certiflow.domain.shared.Person;
import java.time.Instant;
import java.util.Objects;

/** Coordinates recording audit facts without coupling decorators to the storage port. */
public final class AuditTrail {

    private final AuditService service;

    public AuditTrail(AuditService service) {
        this.service = Objects.requireNonNull(service, "service");
    }

    public void record(Object subject, AuditAction action, Person by, Instant at, String detail) {
        service.record(new AuditEntry(subject, action, by, at, detail));
    }
}
