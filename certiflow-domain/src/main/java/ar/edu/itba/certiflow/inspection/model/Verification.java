package ar.edu.itba.certiflow.inspection.model;

import ar.edu.itba.certiflow.inspection.model.exceptions.VerificationNotesRequiredException;
import ar.edu.itba.certiflow.shared.model.Person;
import java.time.Instant;

public record Verification(Person verifier, VerificationResult result, String notes, Instant verifiedAt) {

    public Verification {
        if (result.requiresNotes() && notes.isBlank()) {
            throw new VerificationNotesRequiredException();
        }
    }

    public boolean isAccepted() {
        return result.closesAction();
    }
}
