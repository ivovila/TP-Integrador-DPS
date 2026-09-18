package ar.edu.itba.certiflow.details.evaluation;

import ar.edu.itba.certiflow.domain.evaluation.Answer;

import java.math.BigDecimal;
import java.util.Objects;

public record NumericAnswer(BigDecimal value) implements Answer {

}
