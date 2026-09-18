package ar.edu.itba.certiflow.domain.audit;

import ar.edu.itba.certiflow.domain.shared.Person;
import java.time.Instant;
import java.util.Objects;

public record AuditEntry(Object subject, AuditAction action, Person by, Instant at, String detail) {
    public boolean concerns(Object candidate) {
        return subject.equals(candidate);
    }
}
