package ar.edu.itba.certiflow.domain.evaluation;


import java.util.Objects;

/** The schema fixes the unit; the inspector records only the numeric value. */
public record NumericRangeRule(Unit unit, Range range) implements ApprovalRule<NumericAnswer> {

    public NumericRangeRule {
        Objects.requireNonNull(unit, "unit");
        Objects.requireNonNull(range, "range");
    }

    @Override
    public boolean isSatisfiedBy(NumericAnswer answer) {
        return range.contains(answer.value());
    }
}
