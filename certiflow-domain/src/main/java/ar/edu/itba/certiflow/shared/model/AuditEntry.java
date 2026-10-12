package ar.edu.itba.certiflow.shared.model;

import java.time.Instant;

public record AuditEntry(AuditAction action, Person performedBy, Instant occurredAt, String detail) {
}
