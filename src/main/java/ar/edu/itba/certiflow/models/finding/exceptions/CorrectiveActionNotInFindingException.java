package ar.edu.itba.certiflow.models.finding.exceptions;

import ar.edu.itba.certiflow.models.shared.DomainException;

public class CorrectiveActionNotInFindingException extends DomainException {

    public CorrectiveActionNotInFindingException() {
        super("The corrective action does not belong to this finding");
    }
}
