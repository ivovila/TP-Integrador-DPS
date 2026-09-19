package ar.edu.itba.certiflow.models.finding;

import ar.edu.itba.certiflow.models.shared.Person;
import java.time.Instant;

public record Verification(Person verifier, VerificationResult result, String notes, Instant verifiedAt) {

    public boolean isAccepted() {
        return result.closesAction();
    }
}
