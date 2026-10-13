package ar.edu.itba.certiflow.inspection.model;

import ar.edu.itba.certiflow.shared.model.Person;
import ar.edu.itba.certiflow.shared.model.RequiredValueMissingException;
import ar.edu.itba.certiflow.shared.model.Stamp;
import java.time.Instant;

public record Observation(String text, Stamp observed) {

    public Observation {
        if (text.isBlank()) {
            throw new RequiredValueMissingException("An observation must not be blank");
        }
    }

    public Person observedBy() {
        return observed.by();
    }

    public Instant observedAt() {
        return observed.at();
    }
}
