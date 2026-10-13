package ar.edu.itba.certiflow.schema.model;

import ar.edu.itba.certiflow.schema.model.exceptions.AnswerTypeMismatchException;
import java.math.BigDecimal;

public record NumericRangeRule(Unit unit, Range range) implements ApprovalRule<NumericAnswer> {

    @Override
    public boolean isSatisfiedBy(NumericAnswer answer) {
        return range.contains(answer.value());
    }

    @Override
    public NumericAnswer answerFrom(RawAnswer raw) {
        try {
            return new NumericAnswer(new BigDecimal(raw.text()));
        } catch (NumberFormatException notANumber) {
            throw new AnswerTypeMismatchException("numeric", raw);
        }
    }

    @Override
    public <T> T describedBy(ApprovalRuleDescriber<T> describer) {
        return describer.numericRange(unit, range);
    }
}
