package ar.edu.itba.certiflow.inspection.model.exceptions;

import ar.edu.itba.certiflow.shared.model.DomainException;

public class CorrectiveActionAlreadyClosedException extends DomainException {

    public CorrectiveActionAlreadyClosedException() {
        super("The corrective action was already verified and closed");
    }
}
