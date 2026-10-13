package ar.edu.itba.certiflow.schema.model;

import java.math.BigDecimal;

public record NumericAnswer(BigDecimal value) implements Answer {

    @Override
    public <T> T describedBy(AnswerDescriber<T> describer) {
        return describer.number(value);
    }
}
