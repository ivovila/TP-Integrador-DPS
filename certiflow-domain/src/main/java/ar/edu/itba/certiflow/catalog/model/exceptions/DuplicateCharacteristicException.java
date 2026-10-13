package ar.edu.itba.certiflow.catalog.model.exceptions;

import ar.edu.itba.certiflow.shared.model.InvalidValueException;

public class DuplicateCharacteristicException extends InvalidValueException {

    public DuplicateCharacteristicException(String name) {
        super("Characteristic '" + name + "' is declared more than once");
    }
}
