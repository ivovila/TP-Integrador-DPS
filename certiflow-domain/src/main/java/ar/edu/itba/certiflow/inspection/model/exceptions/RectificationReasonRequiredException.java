package ar.edu.itba.certiflow.inspection.model.exceptions;

import ar.edu.itba.certiflow.shared.model.RuleViolationException;

public class RectificationReasonRequiredException extends RuleViolationException {

    public RectificationReasonRequiredException() {
        super("A rectification must state its reason");
    }
}
