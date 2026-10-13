package ar.edu.itba.certiflow.schema.model;

import java.math.BigDecimal;

public record NumericRangeRule(Unit unit, Range range) implements ApprovalRule<NumericAnswer> {

    @Override
    public boolean isSatisfiedBy(NumericAnswer answer) {
        return range.contains(answer.value());
    }

    @Override
    public NumericAnswer answerFrom(RawAnswer raw) {
        return raw.as(BigDecimal.class).map(NumericAnswer::new)
                .orElseThrow(() -> new AnswerTypeMismatchException("numeric", raw));
    }
}
