package ar.edu.itba.certiflow.domain.certificate;

import ar.edu.itba.certiflow.domain.shared.Person;
import java.time.Instant;
import java.util.Objects;

public record Suspension(String reason, Person by, Instant at) {

    public Suspension {
        if (reason.isBlank()) {
            throw new IllegalArgumentException("A suspension must state its reason");
        }
    }
}
