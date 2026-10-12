package ar.edu.itba.certiflow.web.support;

import ar.edu.itba.certiflow.shared.model.AuditEntry;
import java.time.Instant;
import org.springframework.hateoas.server.core.Relation;

@Relation(collectionRelation = "auditEntries", itemRelation = "auditEntry")
public record AuditEntryResponse(String action, String label, String performedById, String performedByName, Instant occurredAt,
                                 String detail) {

    public static AuditEntryResponse of(AuditEntry entry) {
        return new AuditEntryResponse(((Enum<?>) entry.action()).name(), entry.action().label(), entry.performedBy().id().value(),
                entry.performedBy().name(), entry.occurredAt(), entry.detail());
    }
}
