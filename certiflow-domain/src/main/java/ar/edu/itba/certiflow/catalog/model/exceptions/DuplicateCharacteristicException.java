package ar.edu.itba.certiflow.catalog.model.exceptions;

import ar.edu.itba.certiflow.shared.model.DomainException;

public class DuplicateCharacteristicException extends DomainException {

    public DuplicateCharacteristicException(String name) {
        super("Characteristic '" + name + "' is declared more than once");
    }
}
