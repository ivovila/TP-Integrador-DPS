package ar.edu.itba.certiflow.inspection.model;

import ar.edu.itba.certiflow.inspection.model.exceptions.VerificationNotesRequiredException;
import ar.edu.itba.certiflow.shared.model.Person;
import ar.edu.itba.certiflow.shared.model.Stamp;
import java.time.Instant;

public record Verification(VerificationResult result, String notes, Stamp verified) {

    public Verification {
        if (result.requiresNotes() && notes.isBlank()) {
            throw new VerificationNotesRequiredException();
        }
    }

    public boolean isAccepted() {
        return result.closesAction();
    }

    public Person verifier() {
        return verified.by();
    }

    public Instant verifiedAt() {
        return verified.at();
    }
}
