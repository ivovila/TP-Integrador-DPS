package ar.edu.itba.certiflow.domain;

import java.math.BigDecimal;
import java.util.Objects;

public sealed interface Answer permits Answer.Numeric, Answer.YesNo, Answer.Documentary {
    record Numeric(BigDecimal value, String unit) implements Answer {
        public Numeric {
            Objects.requireNonNull(value);
            unit = Checks.text(unit, "unit");
        }
    }

    record YesNo(boolean value) implements Answer {}

    record Documentary() implements Answer {}
}
