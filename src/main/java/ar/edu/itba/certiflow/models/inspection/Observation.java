package ar.edu.itba.certiflow.models.inspection;

import ar.edu.itba.certiflow.models.shared.Person;
import java.time.Instant;

public record Observation(String text, Person observedBy, Instant observedAt) {

    public Observation {
        if (text.isBlank()) {
            throw new IllegalArgumentException("An observation must not be blank");
        }
    }
}
