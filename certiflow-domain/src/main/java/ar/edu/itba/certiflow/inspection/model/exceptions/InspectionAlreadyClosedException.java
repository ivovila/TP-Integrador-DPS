package ar.edu.itba.certiflow.inspection.model.exceptions;

import ar.edu.itba.certiflow.shared.model.DomainException;

public class InspectionAlreadyClosedException extends DomainException {

    public InspectionAlreadyClosedException() {
        super("A closed inspection only accepts rectifications");
    }
}
