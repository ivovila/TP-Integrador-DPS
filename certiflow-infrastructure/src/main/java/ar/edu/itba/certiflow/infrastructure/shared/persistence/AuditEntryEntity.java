package ar.edu.itba.certiflow.infrastructure.shared.persistence;

import ar.edu.itba.certiflow.shared.model.AuditEntry;
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

    static AuditEntryEntity of(String subjectType, String subjectId, AuditEntry entry) {
        AuditEntryEntity entity = new AuditEntryEntity();
        PersonColumns by = PersonColumns.of(entry.performedBy());
        entity.subjectType = subjectType;
        entity.subjectId = subjectId;
        entity.action = AuditActions.nameOf(entry.action());
        entity.performedById = by.id();
        entity.performedByName = by.name();
        entity.occurredAt = entry.occurredAt();
        entity.detail = entry.detail();
        return entity;
    }

    AuditEntry toDomain() {
        return new AuditEntry(AuditActions.valueOf(action), new PersonColumns(performedById, performedByName).toDomain(), occurredAt,
                detail);
    }
}
