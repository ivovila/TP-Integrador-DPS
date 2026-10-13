package ar.edu.itba.certiflow.inspection.model.exceptions;

import ar.edu.itba.certiflow.shared.model.RuleViolationException;

public class InspectionNotClosedException extends RuleViolationException {

    public InspectionNotClosedException() {
        super("The inspection has not been closed yet");
    }
}
