package ar.edu.itba.certiflow.models.finding.exceptions;

import ar.edu.itba.certiflow.models.shared.DomainException;

public class CorrectiveActionAlreadyClosedException extends DomainException {

    public CorrectiveActionAlreadyClosedException() {
        super("The corrective action was already verified and closed");
    }
}
