package ar.edu.itba.certiflow.models.audit;

import ar.edu.itba.certiflow.models.shared.Person;
import java.time.Instant;

public record AuditEntry(Object subject, AuditAction action, Person performedBy, Instant occurredAt, String detail) {
    public boolean concerns(Object candidate) {
        return subject.equals(candidate);
    }
}
