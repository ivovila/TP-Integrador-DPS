package ar.edu.itba.certiflow.models.schema.exceptions;

import ar.edu.itba.certiflow.models.shared.DomainException;

public class InvalidSchemaException extends DomainException {

    public InvalidSchemaException(String message) {
        super(message);
    }
}
