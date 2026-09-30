package ar.edu.itba.certiflow.models.inspection.exceptions;

import ar.edu.itba.certiflow.models.shared.DomainException;

public class InspectionAlreadyClosedException extends DomainException {

    public InspectionAlreadyClosedException() {
        super("A closed inspection only accepts rectifications");
    }
}
