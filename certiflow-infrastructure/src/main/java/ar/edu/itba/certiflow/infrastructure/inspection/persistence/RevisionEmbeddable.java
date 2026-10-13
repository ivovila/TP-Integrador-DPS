package ar.edu.itba.certiflow.infrastructure.inspection.persistence;

import ar.edu.itba.certiflow.infrastructure.shared.persistence.PersonColumns;
import ar.edu.itba.certiflow.inspection.model.Revision;
import ar.edu.itba.certiflow.schema.model.SchemaVersion;
import ar.edu.itba.certiflow.shared.model.Stamp;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.Instant;

@Embeddable
public class RevisionEmbeddable {

    private int number;
    private String reason;
    @Column(name = "sealed_by_id")
    private String sealedById;
    @Column(name = "sealed_by_name")
    private String sealedByName;
    @Column(name = "sealed_at")
    private Instant sealedAt;
    private String evaluation;

    protected RevisionEmbeddable() {
    }

    RevisionEmbeddable(Revision revision, EvaluationDocumentMapper evaluationDocuments) {
        PersonColumns sealedBy = new PersonColumns(revision.sealedBy());
        this.number = revision.number();
        this.reason = revision.reason();
        this.sealedById = sealedBy.id();
        this.sealedByName = sealedBy.name();
        this.sealedAt = revision.sealedAt();
        this.evaluation = evaluationDocuments.write(revision.evaluation());
    }

    Revision toDomain(SchemaVersion version, EvaluationDocumentMapper evaluationDocuments) {
        return new Revision(number, reason, evaluationDocuments.read(evaluation, version),
                new Stamp(new PersonColumns(sealedById, sealedByName).toDomain(), sealedAt));
    }
}
