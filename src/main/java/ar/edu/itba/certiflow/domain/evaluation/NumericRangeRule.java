package ar.edu.itba.certiflow.domain.evaluation;


import java.util.Objects;

public record NumericRangeRule(Unit unit, Range range) implements ApprovalRule<NumericAnswer> {


    @Override
    public boolean isSatisfiedBy(NumericAnswer answer) {
        return range.contains(answer.value());
    }
}
