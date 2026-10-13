package ar.edu.itba.certiflow.inspection.model.exceptions;

import ar.edu.itba.certiflow.shared.model.RuleViolationException;

public class CorrectiveActionAlreadyClosedException extends RuleViolationException {

    public CorrectiveActionAlreadyClosedException() {
        super("The corrective action was already verified and closed");
    }
}
