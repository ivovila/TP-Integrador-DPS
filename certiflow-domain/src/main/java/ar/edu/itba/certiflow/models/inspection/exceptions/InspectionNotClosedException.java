package ar.edu.itba.certiflow.models.inspection.exceptions;

import ar.edu.itba.certiflow.models.shared.DomainException;

public class InspectionNotClosedException extends DomainException {

    public InspectionNotClosedException() {
        super("The inspection has not been closed yet");
    }
}
