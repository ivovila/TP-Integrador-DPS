package ar.edu.itba.certiflow.domain.inspection.exceptions;

import ar.edu.itba.certiflow.domain.shared.DomainException;

public class RectificationReasonRequiredException extends DomainException {

    public RectificationReasonRequiredException() {
        super("A rectification must state its reason");
    }
}
