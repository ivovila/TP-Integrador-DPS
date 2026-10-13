package ar.edu.itba.certiflow.infrastructure.shared.persistence;

import ar.edu.itba.certiflow.shared.model.AuditAction;
import ar.edu.itba.certiflow.shared.model.AuditActionCatalog;
import ar.edu.itba.certiflow.shared.model.AuditEntry;
import ar.edu.itba.certiflow.shared.model.Stamp;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "audit_entry")
public class AuditEntryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "subject_type")
    private String subjectType;
    @Column(name = "subject_id")
    private String subjectId;
    private String action;
    @Column(name = "performed_by_id")
    private String performedById;
    @Column(name = "performed_by_name")
    private String performedByName;
    @Column(name = "occurred_at")
    private Instant occurredAt;
    private String detail;

    protected AuditEntryEntity() {
    }

    AuditEntryEntity(String subjectType, String subjectId, AuditEntry entry) {
        PersonColumns performedBy = new PersonColumns(entry.performedBy());
        this.subjectType = subjectType;
        this.subjectId = subjectId;
        this.action = entry.action().code();
        this.performedById = performedBy.id();
        this.performedByName = performedBy.name();
        this.occurredAt = entry.occurredAt();
        this.detail = entry.detail();
    }

    AuditEntry toDomain(AuditActionCatalog auditActions) {
        AuditAction storedAction = auditActions.withCode(action)
                .orElseThrow(() -> new UnreadableStoredDataException("audit action", action));
        return new AuditEntry(storedAction,
                new Stamp(new PersonColumns(performedById, performedByName).toDomain(), occurredAt), detail);
    }
}
