package ar.edu.itba.certiflow.schema.model;

import java.math.BigDecimal;

public record NumericAnswer(BigDecimal value) implements Answer {

    @Override
    public RawAnswer raw() {
        return new RawAnswer.Number(value);
    }
}
