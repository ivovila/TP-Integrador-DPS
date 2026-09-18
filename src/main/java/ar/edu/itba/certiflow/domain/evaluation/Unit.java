package ar.edu.itba.certiflow.domain.evaluation;

import java.util.Objects;

public record Unit(String symbol) {

    public Unit {
        if (symbol.isBlank()) {
            throw new IllegalArgumentException("A unit needs a symbol");
        }
    }
}
