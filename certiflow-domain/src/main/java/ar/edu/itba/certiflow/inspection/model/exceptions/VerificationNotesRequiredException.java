package ar.edu.itba.certiflow.inspection.model.exceptions;

import ar.edu.itba.certiflow.shared.model.InvalidValueException;

public class VerificationNotesRequiredException extends InvalidValueException {

    public VerificationNotesRequiredException() {
        super("A rejected verification must explain why the corrective action was not accepted");
    }
}
