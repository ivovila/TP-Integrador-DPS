package ar.edu.itba.certiflow.inspection.model.exceptions;

import ar.edu.itba.certiflow.shared.model.DomainException;

public class RectificationReasonRequiredException extends DomainException {

    public RectificationReasonRequiredException() {
        super("A rectification must state its reason");
    }
}
