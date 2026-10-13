package ar.edu.itba.certiflow.certification.model.exceptions;

import ar.edu.itba.certiflow.shared.model.DomainException;

public class InvalidValidityTermException extends DomainException {

    public InvalidValidityTermException(int length) {
        super("A validity term must be positive, not " + length);
    }
}
