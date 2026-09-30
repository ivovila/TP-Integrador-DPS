package ar.edu.itba.certiflow.models.finding.exceptions;

import ar.edu.itba.certiflow.models.shared.DomainException;

public class VerificationNotesRequiredException extends DomainException {

    public VerificationNotesRequiredException() {
        super("A rejected verification must explain why the corrective action was not accepted");
    }
}
