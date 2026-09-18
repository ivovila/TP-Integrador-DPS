package ar.edu.itba.certiflow.domain.finding;

import ar.edu.itba.certiflow.domain.shared.DomainException;

public class CorrectiveActionNotInFindingException extends DomainException {

    public CorrectiveActionNotInFindingException() {
        super("The corrective action does not belong to this finding");
    }
}
