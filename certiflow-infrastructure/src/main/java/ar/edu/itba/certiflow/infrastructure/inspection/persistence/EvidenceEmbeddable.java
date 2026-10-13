package ar.edu.itba.certiflow.infrastructure.inspection.persistence;

import ar.edu.itba.certiflow.infrastructure.shared.persistence.PersonColumns;
import ar.edu.itba.certiflow.infrastructure.shared.persistence.UnreadableStoredDataException;
import ar.edu.itba.certiflow.inspection.model.AttachedEvidence;
import ar.edu.itba.certiflow.schema.model.Evidence;
import ar.edu.itba.certiflow.schema.model.EvidenceKind;
import ar.edu.itba.certiflow.schema.model.EvidenceKindCatalog;
import ar.edu.itba.certiflow.shared.model.Stamp;
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

    EvidenceEmbeddable(AttachedEvidence attached) {
        PersonColumns attachedBy = new PersonColumns(attached.attachedBy());
        this.kind = attached.evidence().kind().code();
        this.reference = attached.evidence().reference();
        this.description = attached.evidence().description();
        this.attachedById = attachedBy.id();
        this.attachedByName = attachedBy.name();
        this.attachedAt = attached.attachedAt();
    }

    AttachedEvidence toDomain(EvidenceKindCatalog evidenceKinds) {
        EvidenceKind evidenceKind = evidenceKinds.withCode(kind)
                .orElseThrow(() -> new UnreadableStoredDataException("evidence kind", kind));
        return new AttachedEvidence(new Evidence(evidenceKind, reference, description),
                new Stamp(new PersonColumns(attachedById, attachedByName).toDomain(), attachedAt));
    }
}
