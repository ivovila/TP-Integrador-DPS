package ar.edu.itba.certiflow.models.evaluation;

import java.math.BigDecimal;

public record NumericAnswer(BigDecimal value) implements Answer {

}
