package ar.edu.itba.certiflow.inspection.model;

import ar.edu.itba.certiflow.shared.model.Person;
import ar.edu.itba.certiflow.shared.model.RequiredValueMissingException;
import java.time.Instant;

public record Observation(String text, Person observedBy, Instant observedAt) {

    public Observation {
        if (text.isBlank()) {
            throw new RequiredValueMissingException("An observation must not be blank");
        }
    }
}
