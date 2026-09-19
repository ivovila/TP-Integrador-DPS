package ar.edu.itba.certiflow.models.audit;

import ar.edu.itba.certiflow.models.shared.Person;
import java.time.Instant;

public record AuditEntry(AuditAction action, Person performedBy, Instant occurredAt, String detail) {
}
