package ar.edu.itba.certiflow.details.evaluation;

import ar.edu.itba.certiflow.domain.evaluation.ApprovalRule;
import ar.edu.itba.certiflow.domain.evaluation.Unit;

import java.util.Objects;

public record NumericRangeRule(Unit unit, Range range) implements ApprovalRule<Measurement> {

    public NumericRangeRule {
        Objects.requireNonNull(unit, "unit");
        Objects.requireNonNull(range, "range");
    }

    @Override
    public boolean isSatisfiedBy(Measurement measurement) {
        return range.contains(measurement.valueIn(unit));
    }
}
