package ar.edu.itba.certiflow.details.evaluation;

import ar.edu.itba.certiflow.domain.evaluation.Answer;
import ar.edu.itba.certiflow.domain.evaluation.Unit;
import ar.edu.itba.certiflow.domain.evaluation.UnitMismatchException;

import java.math.BigDecimal;
import java.util.Objects;

public record Measurement(BigDecimal value, Unit unit) implements Answer {

    public Measurement {
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(unit, "unit");
    }

    /** No hay conversión entre unidades: pedir el valor en otra unidad es un error de carga. */
    public BigDecimal valueIn(Unit expected) {
        if (!unit.equals(expected)) {
            throw new UnitMismatchException(expected, unit);
        }
        return value;
    }
}
