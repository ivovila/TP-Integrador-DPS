package ar.edu.itba.certiflow.models.evaluation;

import ar.edu.itba.certiflow.models.shared.RequiredValueMissingException;

public record Unit(String symbol) {

    public Unit {
        if (symbol.isBlank()) {
            throw new RequiredValueMissingException("A unit needs a symbol");
        }
    }
}
