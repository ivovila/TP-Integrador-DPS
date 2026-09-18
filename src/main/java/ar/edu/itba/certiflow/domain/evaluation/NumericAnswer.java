package ar.edu.itba.certiflow.domain.evaluation;

import java.math.BigDecimal;
import java.util.Objects;

public record NumericAnswer(BigDecimal value) implements Answer {

}
