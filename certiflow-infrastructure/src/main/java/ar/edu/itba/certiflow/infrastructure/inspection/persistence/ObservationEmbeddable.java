package ar.edu.itba.certiflow.infrastructure.inspection.persistence;

import ar.edu.itba.certiflow.shared.model.Stamp;
import ar.edu.itba.certiflow.infrastructure.shared.persistence.PersonColumns;
import ar.edu.itba.certiflow.inspection.model.Observation;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.Instant;

@Embeddable
public class ObservationEmbeddable {

    private String text;
    @Column(name = "observed_by_id")
    private String observedById;
    @Column(name = "observed_by_name")
    private String observedByName;
    @Column(name = "observed_at")
    private Instant observedAt;

    protected ObservationEmbeddable() {
    }

    ObservationEmbeddable(Observation observation) {
        PersonColumns observedBy = new PersonColumns(observation.observedBy());
        this.text = observation.text();
        this.observedById = observedBy.id();
        this.observedByName = observedBy.name();
        this.observedAt = observation.observedAt();
    }

    Observation toDomain() {
        return new Observation(text, new Stamp(new PersonColumns(observedById, observedByName).toDomain(), observedAt));
    }
}
