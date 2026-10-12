package ar.edu.itba.certiflow.schema.model;

import ar.edu.itba.certiflow.shared.model.RequiredValueMissingException;

public record Unit(String symbol) {

    public Unit {
        if (symbol.isBlank()) {
            throw new RequiredValueMissingException("A unit needs a symbol");
        }
    }
}
