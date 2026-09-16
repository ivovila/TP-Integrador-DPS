package ar.edu.itba.certiflow.domain.rule;

import ar.edu.itba.certiflow.domain.Answer;
import ar.edu.itba.certiflow.domain.Checks;
import ar.edu.itba.certiflow.domain.Evaluation;
import ar.edu.itba.certiflow.domain.Outcome;

import java.math.BigDecimal;
import java.util.Objects;

/** Inclusive ranges. Accepted but outside preferred is observed; outside accepted is rejected. */
public record NumericRangeRule(
        BigDecimal minimum,
        BigDecimal preferredMinimum,
        BigDecimal preferredMaximum,
        BigDecimal maximum,
        String unit)
        implements EvaluationRule {
    public NumericRangeRule {
        Objects.requireNonNull(minimum);
        Objects.requireNonNull(preferredMinimum);
        Objects.requireNonNull(preferredMaximum);
        Objects.requireNonNull(maximum);
        unit = Checks.text(unit, "unit");
        Checks.require(
                minimum.compareTo(preferredMinimum) <= 0
                        && preferredMinimum.compareTo(preferredMaximum) <= 0
                        && preferredMaximum.compareTo(maximum) <= 0,
                "Ranges must be ordered and nested");
    }

    public Evaluation evaluate(Answer answer) {
        Checks.require(answer instanceof Answer.Numeric, "Expected a numeric answer");
        var numeric = (Answer.Numeric) answer;
        Checks.require(unit.equals(numeric.unit()), "Measurement unit does not match criterion");
        var value = numeric.value();
        Outcome outcome =
                value.compareTo(minimum) < 0 || value.compareTo(maximum) > 0
                        ? Outcome.REJECTED
                        : value.compareTo(preferredMinimum) < 0
                                        || value.compareTo(preferredMaximum) > 0
                                ? Outcome.OBSERVED
                                : Outcome.APPROVED;
        return new Evaluation(
                outcome,
                "Measured "
                        + value
                        + " "
                        + unit
                        + "; accepted ["
                        + minimum
                        + ", "
                        + maximum
                        + "], preferred ["
                        + preferredMinimum
                        + ", "
                        + preferredMaximum
                        + "]");
    }
}
