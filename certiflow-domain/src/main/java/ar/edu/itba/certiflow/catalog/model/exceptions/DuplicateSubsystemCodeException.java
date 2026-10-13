package ar.edu.itba.certiflow.catalog.model.exceptions;

import ar.edu.itba.certiflow.shared.model.InvalidValueException;

public class DuplicateSubsystemCodeException extends InvalidValueException {

    public DuplicateSubsystemCodeException() {
        super("Subsystem codes must be unique within an asset");
    }
}
