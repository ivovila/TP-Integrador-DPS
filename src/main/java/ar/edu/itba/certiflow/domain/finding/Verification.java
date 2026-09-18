package ar.edu.itba.certiflow.domain.finding;

import ar.edu.itba.certiflow.domain.shared.Person;
import java.time.Instant;
import java.util.Objects;

public record Verification(Person verifier, VerificationResult result, String notes, Instant at) {

    public Verification {
        Objects.requireNonNull(verifier, "verifier");
        Objects.requireNonNull(result, "result");
        Objects.requireNonNull(notes, "notes");
        Objects.requireNonNull(at, "at");
    }

    public boolean isAccepted() {
        return result.closesAction();
    }
}
