package ar.edu.itba.certiflow.domain.evaluation;

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
