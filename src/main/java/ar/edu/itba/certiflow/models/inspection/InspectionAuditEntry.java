package ar.edu.itba.certiflow.models.inspection;

import ar.edu.itba.certiflow.models.people.Inspector;
import java.time.Instant;
import java.util.UUID;

public record InspectionAuditEntry(UUID id, UUID eventId, Inspector actor, Instant occurredAt, String detail) {
}
