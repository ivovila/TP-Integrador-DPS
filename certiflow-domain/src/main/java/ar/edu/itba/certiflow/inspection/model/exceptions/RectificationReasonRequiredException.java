package ar.edu.itba.certiflow.inspection.model.exceptions;

import ar.edu.itba.certiflow.shared.model.InvalidValueException;

public class RectificationReasonRequiredException extends InvalidValueException {

    public RectificationReasonRequiredException() {
        super("A rectification must state its reason");
    }
}
