package ar.edu.itba.certiflow.models.asset.exceptions;

import ar.edu.itba.certiflow.models.shared.DomainException;

public class DuplicateCharacteristicException extends DomainException {

    public DuplicateCharacteristicException(String name) {
        super("Characteristic '" + name + "' is declared more than once");
    }
}
