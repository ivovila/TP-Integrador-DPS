package ar.edu.itba.certiflow.certification.model;

import ar.edu.itba.certiflow.shared.model.Stamp;
import ar.edu.itba.certiflow.shared.model.Person;
import ar.edu.itba.certiflow.shared.model.RequiredValueMissingException;
import java.time.Instant;

public record Suspension(String reason, Stamp suspended) {

    public Suspension {
        if (reason.isBlank()) {
            throw new RequiredValueMissingException("A suspension must state its reason");
        }
    }

    public Person suspendedBy() {
        return suspended.author();
    }

    public Instant suspendedAt() {
        return suspended.instant();
    }
}
