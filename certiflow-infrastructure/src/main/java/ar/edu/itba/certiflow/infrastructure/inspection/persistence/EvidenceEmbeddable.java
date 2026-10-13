package ar.edu.itba.certiflow.infrastructure.inspection.persistence;

import ar.edu.itba.certiflow.shared.model.Stamp;
import ar.edu.itba.certiflow.infrastructure.shared.persistence.PersonColumns;
import ar.edu.itba.certiflow.inspection.model.AttachedEvidence;
import ar.edu.itba.certiflow.schema.model.Evidence;
import ar.edu.itba.certiflow.schema.model.StandardEvidenceKind;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.Instant;

@Embeddable
public class EvidenceEmbeddable {

    private String kind;
    private String reference;
    private String description;
    @Column(name = "attached_by_id")
    private String attachedById;
    @Column(name = "attached_by_name")
    private String attachedByName;
    @Column(name = "attached_at")
    private Instant attachedAt;

    protected EvidenceEmbeddable() {
    }

    static EvidenceEmbeddable of(AttachedEvidence attached) {
        EvidenceEmbeddable embeddable = new EvidenceEmbeddable();
        PersonColumns by = PersonColumns.of(attached.attachedBy());
        embeddable.kind = attached.evidence().kind().code();
        embeddable.reference = attached.evidence().reference();
        embeddable.description = attached.evidence().description();
        embeddable.attachedById = by.id();
        embeddable.attachedByName = by.name();
        embeddable.attachedAt = attached.attachedAt();
        return embeddable;
    }

    AttachedEvidence toDomain() {
        return new AttachedEvidence(new Evidence(StandardEvidenceKind.valueOf(kind), reference, description),
                new Stamp(new PersonColumns(attachedById, attachedByName).toDomain(), attachedAt));
    }
}
