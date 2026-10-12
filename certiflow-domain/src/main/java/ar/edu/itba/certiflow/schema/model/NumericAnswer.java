package ar.edu.itba.certiflow.schema.model;

import java.math.BigDecimal;

public record NumericAnswer(BigDecimal value) implements Answer {

}
