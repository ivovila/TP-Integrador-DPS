package ar.edu.itba.certiflow.infrastructure.certification.persistence;

import ar.edu.itba.certiflow.shared.model.Stamp;
import ar.edu.itba.certiflow.certification.model.Suspension;
import ar.edu.itba.certiflow.infrastructure.shared.persistence.PersonColumns;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.Instant;

@Embeddable
public class SuspensionEmbeddable {

    private String reason;
    @Column(name = "suspended_by_id")
    private String suspendedById;
    @Column(name = "suspended_by_name")
    private String suspendedByName;
    @Column(name = "suspended_at")
    private Instant suspendedAt;

    protected SuspensionEmbeddable() {
    }

    static SuspensionEmbeddable of(Suspension suspension) {
        SuspensionEmbeddable embeddable = new SuspensionEmbeddable();
        PersonColumns by = PersonColumns.of(suspension.suspendedBy());
        embeddable.reason = suspension.reason();
        embeddable.suspendedById = by.id();
        embeddable.suspendedByName = by.name();
        embeddable.suspendedAt = suspension.suspendedAt();
        return embeddable;
    }

    Suspension toDomain() {
        return new Suspension(reason, new Stamp(new PersonColumns(suspendedById, suspendedByName).toDomain(), suspendedAt));
    }
}
