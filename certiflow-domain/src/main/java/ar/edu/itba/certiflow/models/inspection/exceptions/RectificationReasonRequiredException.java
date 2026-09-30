package ar.edu.itba.certiflow.models.inspection.exceptions;

import ar.edu.itba.certiflow.models.shared.DomainException;

public class RectificationReasonRequiredException extends DomainException {

    public RectificationReasonRequiredException() {
        super("A rectification must state its reason");
    }
}
