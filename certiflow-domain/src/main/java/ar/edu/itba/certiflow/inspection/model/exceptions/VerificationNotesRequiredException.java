package ar.edu.itba.certiflow.inspection.model.exceptions;

import ar.edu.itba.certiflow.shared.model.DomainException;

public class VerificationNotesRequiredException extends DomainException {

    public VerificationNotesRequiredException() {
        super("A rejected verification must explain why the corrective action was not accepted");
    }
}
