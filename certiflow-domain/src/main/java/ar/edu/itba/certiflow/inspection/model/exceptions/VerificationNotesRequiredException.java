package ar.edu.itba.certiflow.inspection.model.exceptions;

import ar.edu.itba.certiflow.shared.model.RuleViolationException;

public class VerificationNotesRequiredException extends RuleViolationException {

    public VerificationNotesRequiredException() {
        super("A rejected verification must explain why the corrective action was not accepted");
    }
}
