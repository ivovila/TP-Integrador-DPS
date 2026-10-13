package ar.edu.itba.certiflow.certification.model.exceptions;

import ar.edu.itba.certiflow.shared.model.InvalidValueException;

public class InvalidValidityTermException extends InvalidValueException {

    public InvalidValidityTermException(int length) {
        super("A validity term must be positive, not " + length);
    }
}
