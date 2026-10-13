package ar.edu.itba.certiflow.inspection.model.exceptions;

import ar.edu.itba.certiflow.shared.model.RuleViolationException;

public class InspectionAlreadyClosedException extends RuleViolationException {

    public InspectionAlreadyClosedException() {
        super("A closed inspection only accepts rectifications");
    }
}
