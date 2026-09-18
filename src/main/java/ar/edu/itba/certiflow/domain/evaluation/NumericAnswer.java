package ar.edu.itba.certiflow.details.evaluation;

import ar.edu.itba.certiflow.domain.evaluation.Answer;

import java.math.BigDecimal;

public record NumericAnswer(BigDecimal value) implements Answer {

}
