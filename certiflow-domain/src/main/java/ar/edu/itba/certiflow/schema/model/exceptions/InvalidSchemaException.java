package ar.edu.itba.certiflow.schema.model.exceptions;

import ar.edu.itba.certiflow.shared.model.InvalidValueException;

public class InvalidSchemaException extends InvalidValueException {

    public InvalidSchemaException(String message) {
        super(message);
    }
}
