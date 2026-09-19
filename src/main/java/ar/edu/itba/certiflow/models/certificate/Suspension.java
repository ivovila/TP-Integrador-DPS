package ar.edu.itba.certiflow.models.certificate;

import ar.edu.itba.certiflow.models.shared.Person;
import java.time.Instant;

public record Suspension(String reason, Person suspendedBy, Instant suspendedAt) {

    public Suspension {
        if (reason.isBlank()) {
            throw new IllegalArgumentException("A suspension must state its reason");
        }
    }
}
