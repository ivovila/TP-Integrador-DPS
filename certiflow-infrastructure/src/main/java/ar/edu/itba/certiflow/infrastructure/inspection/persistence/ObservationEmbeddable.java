package ar.edu.itba.certiflow.infrastructure.inspection.persistence;

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

    static ObservationEmbeddable of(Observation observation) {
        ObservationEmbeddable embeddable = new ObservationEmbeddable();
        PersonColumns by = PersonColumns.of(observation.observedBy());
        embeddable.text = observation.text();
        embeddable.observedById = by.id();
        embeddable.observedByName = by.name();
        embeddable.observedAt = observation.observedAt();
        return embeddable;
    }

    Observation toDomain() {
        return new Observation(text, new PersonColumns(observedById, observedByName).toDomain(), observedAt);
    }
}
