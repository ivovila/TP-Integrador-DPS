package ar.edu.itba.certiflow.domain.finding;

import ar.edu.itba.certiflow.domain.shared.DomainException;

public class CorrectiveActionAlreadyClosedException extends DomainException {

    public CorrectiveActionAlreadyClosedException() {
        super("The corrective action was already verified and closed");
    }
}
