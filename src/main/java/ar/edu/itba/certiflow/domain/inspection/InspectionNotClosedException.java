package ar.edu.itba.certiflow.domain.inspection;

import ar.edu.itba.certiflow.domain.shared.DomainException;

public class InspectionNotClosedException extends DomainException {

    public InspectionNotClosedException() {
        super("The inspection has not been closed yet");
    }
}
