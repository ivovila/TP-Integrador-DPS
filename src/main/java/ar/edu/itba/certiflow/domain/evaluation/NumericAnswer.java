package ar.edu.itba.certiflow.domain.evaluation;

import java.math.BigDecimal;
import java.util.Objects;

/** Numeric response to a criterion whose unit is defined by the schema rule. */
public record NumericAnswer(BigDecimal value) implements Answer {

    public NumericAnswer {
        Objects.requireNonNull(value, "value");
    }
}
