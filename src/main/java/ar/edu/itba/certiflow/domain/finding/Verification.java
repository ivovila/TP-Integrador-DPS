package ar.edu.itba.certiflow.domain.finding;

import ar.edu.itba.certiflow.domain.shared.Person;
import java.time.Instant;

public record Verification(Person verifier, VerificationResult result, String notes, Instant at) {

    public boolean isAccepted() {
        return result.closesAction();
    }
}
