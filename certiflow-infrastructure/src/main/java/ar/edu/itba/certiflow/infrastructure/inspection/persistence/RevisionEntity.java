package ar.edu.itba.certiflow.infrastructure.inspection.persistence;

import ar.edu.itba.certiflow.infrastructure.shared.persistence.PersonColumns;
import ar.edu.itba.certiflow.inspection.model.Revision;
import ar.edu.itba.certiflow.schema.model.SchemaVersion;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.Instant;

@Embeddable
public class RevisionEntity {

    private int number;
    private String reason;
    @Column(name = "sealed_by_id")
    private String sealedById;
    @Column(name = "sealed_by_name")
    private String sealedByName;
    @Column(name = "sealed_at")
    private Instant sealedAt;
    private String evaluation;

    protected RevisionEntity() {
    }

    static RevisionEntity from(Revision revision) {
        RevisionEntity entity = new RevisionEntity();
        PersonColumns by = PersonColumns.of(revision.sealedBy());
        entity.number = revision.number();
        entity.reason = revision.reason();
        entity.sealedById = by.id();
        entity.sealedByName = by.name();
        entity.sealedAt = revision.sealedAt();
        entity.evaluation = EvaluationJson.write(revision.evaluation());
        return entity;
    }

    Revision toDomain(SchemaVersion version) {
        return new Revision(number, reason, EvaluationJson.read(evaluation, version),
                new PersonColumns(sealedById, sealedByName).toDomain(), sealedAt);
    }
}
