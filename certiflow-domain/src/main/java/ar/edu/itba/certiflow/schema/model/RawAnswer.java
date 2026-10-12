package ar.edu.itba.certiflow.schema.model;

import java.math.BigDecimal;

public sealed interface RawAnswer {

    record Number(BigDecimal value) implements RawAnswer {
    }

    record YesNo(boolean value) implements RawAnswer {
    }

    record Option(String value) implements RawAnswer {
    }
}
