package ar.edu.itba.certiflow.certification.model;

import ar.edu.itba.certiflow.shared.model.Person;
import ar.edu.itba.certiflow.shared.model.RequiredValueMissingException;
import java.time.Instant;

public record Suspension(String reason, Person suspendedBy, Instant suspendedAt) {

    public Suspension {
        if (reason.isBlank()) {
            throw new RequiredValueMissingException("A suspension must state its reason");
        }
    }
}
