package ar.edu.itba.certiflow.schema.model.exceptions;

import ar.edu.itba.certiflow.shared.model.DomainException;

public class InvalidSchemaException extends DomainException {

    public InvalidSchemaException(String message) {
        super(message);
    }
}
