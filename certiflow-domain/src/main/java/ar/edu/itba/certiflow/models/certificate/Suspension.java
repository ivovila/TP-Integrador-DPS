package ar.edu.itba.certiflow.models.certificate;

import ar.edu.itba.certiflow.models.shared.Person;
import ar.edu.itba.certiflow.models.shared.RequiredValueMissingException;
import java.time.Instant;

public record Suspension(String reason, Person suspendedBy, Instant suspendedAt) {

    public Suspension {
        if (reason.isBlank()) {
            throw new RequiredValueMissingException("A suspension must state its reason");
        }
    }
}
