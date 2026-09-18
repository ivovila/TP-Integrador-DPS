package ar.edu.itba.certiflow.domain.evaluation;

import java.math.BigDecimal;

public record NumericAnswer(BigDecimal value) implements Answer {

}
