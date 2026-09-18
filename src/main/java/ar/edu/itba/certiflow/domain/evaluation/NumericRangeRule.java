package ar.edu.itba.certiflow.domain.evaluation;

public record NumericRangeRule(Unit unit, Range range) implements ApprovalRule<NumericAnswer> {

    @Override
    public boolean isSatisfiedBy(NumericAnswer answer) {
        return range.contains(answer.value());
    }
}
