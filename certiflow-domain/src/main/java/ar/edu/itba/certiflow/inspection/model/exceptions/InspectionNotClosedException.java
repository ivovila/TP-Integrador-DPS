package ar.edu.itba.certiflow.inspection.model.exceptions;

import ar.edu.itba.certiflow.shared.model.DomainException;

public class InspectionNotClosedException extends DomainException {

    public InspectionNotClosedException() {
        super("The inspection has not been closed yet");
    }
}
