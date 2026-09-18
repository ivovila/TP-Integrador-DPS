package ar.edu.itba.certiflow.domain.inspection.exceptions;

import ar.edu.itba.certiflow.domain.shared.DomainException;

public class InspectionAlreadyClosedException extends DomainException {

    public InspectionAlreadyClosedException() {
        super("A closed inspection only accepts rectifications");
    }
}
