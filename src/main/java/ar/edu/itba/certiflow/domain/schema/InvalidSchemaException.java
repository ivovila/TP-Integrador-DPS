package ar.edu.itba.certiflow.domain.schema;

import ar.edu.itba.certiflow.domain.shared.DomainException;

public class InvalidSchemaException extends DomainException {

    public InvalidSchemaException(String message) {
        super(message);
    }
}
