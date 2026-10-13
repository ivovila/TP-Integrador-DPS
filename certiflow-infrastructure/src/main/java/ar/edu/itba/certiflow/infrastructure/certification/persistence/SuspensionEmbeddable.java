package ar.edu.itba.certiflow.infrastructure.certification.persistence;

import ar.edu.itba.certiflow.certification.model.Suspension;
import ar.edu.itba.certiflow.infrastructure.shared.persistence.PersonColumns;
import ar.edu.itba.certiflow.shared.model.Stamp;
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

    SuspensionEmbeddable(Suspension suspension) {
        PersonColumns suspendedBy = new PersonColumns(suspension.suspendedBy());
        this.reason = suspension.reason();
        this.suspendedById = suspendedBy.id();
        this.suspendedByName = suspendedBy.name();
        this.suspendedAt = suspension.suspendedAt();
    }

    Suspension toDomain() {
        return new Suspension(reason, new Stamp(new PersonColumns(suspendedById, suspendedByName).toDomain(), suspendedAt));
    }
}
