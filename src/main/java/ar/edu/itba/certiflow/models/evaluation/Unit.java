package ar.edu.itba.certiflow.models.evaluation;

public record Unit(String symbol) {

    public Unit {
        if (symbol.isBlank()) {
            throw new IllegalArgumentException("A unit needs a symbol");
        }
    }
}
