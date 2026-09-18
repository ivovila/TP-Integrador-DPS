package ar.edu.itba.certiflow.domain.evaluation;

import java.util.Objects;

public record Unit(String symbol) {

    public Unit {
        Objects.requireNonNull(symbol, "symbol");
        if (symbol.isBlank()) {
            throw new IllegalArgumentException("A unit needs a symbol");
        }
    }
}
