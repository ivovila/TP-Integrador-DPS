package ar.edu.itba.certiflow.domain.inspection;

import ar.edu.itba.certiflow.domain.shared.Person;
import java.time.Instant;
import java.util.Objects;

public record Observation(String text, Person by, Instant at) {

    public Observation {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(by, "by");
        Objects.requireNonNull(at, "at");
        if (text.isBlank()) {
            throw new IllegalArgumentException("An observation must not be blank");
        }
    }
}
