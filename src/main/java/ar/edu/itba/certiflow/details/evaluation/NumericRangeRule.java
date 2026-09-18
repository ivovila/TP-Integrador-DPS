package ar.edu.itba.certiflow.details.evaluation;

import ar.edu.itba.certiflow.domain.evaluation.ApprovalRule;
import ar.edu.itba.certiflow.domain.evaluation.NumericAnswer;
import ar.edu.itba.certiflow.domain.evaluation.Range;

public record NumericRangeRule(Unit unit, Range range) implements ApprovalRule<NumericAnswer> {

    @Override
    public boolean isSatisfiedBy(NumericAnswer answer) {
        return range.contains(answer.value());
    }
}
