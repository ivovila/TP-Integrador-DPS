package ar.edu.itba.certiflow.assistance.model.exceptions;

import ar.edu.itba.certiflow.shared.model.InvalidValueException;

public class DismissalReasonRequiredException extends InvalidValueException {

    public DismissalReasonRequiredException() {
        super("Dismissing a suggestion must state the reason");
    }
}
