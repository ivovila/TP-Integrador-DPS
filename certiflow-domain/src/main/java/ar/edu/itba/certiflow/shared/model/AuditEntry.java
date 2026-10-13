package ar.edu.itba.certiflow.shared.model;

import java.time.Instant;

public record AuditEntry(AuditAction action, Stamp performed, String detail) {

    public Person performedBy() {
        return performed.by();
    }

    public Instant occurredAt() {
        return performed.at();
    }
}
