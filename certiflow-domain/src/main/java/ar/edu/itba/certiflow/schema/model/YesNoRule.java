package ar.edu.itba.certiflow.schema.model;

import ar.edu.itba.certiflow.schema.model.exceptions.AnswerTypeMismatchException;
import java.util.Arrays;

public record YesNoRule(YesNoAnswer expected) implements ApprovalRule<YesNoAnswer> {

    @Override
    public boolean isSatisfiedBy(YesNoAnswer answer) {
        return expected == answer;
    }

    @Override
    public YesNoAnswer answerFrom(RawAnswer raw) {
        return Arrays.stream(YesNoAnswer.values())
                .filter(candidate -> candidate.isWrittenAs(raw))
                .findFirst()
                .orElseThrow(() -> new AnswerTypeMismatchException("yes/no", raw));
    }

    @Override
    public <T> T describedBy(RuleDescriber<T> describer) {
        return describer.text(YesNoRuleKind.EXPECTED, expected.name()).ofKind(YesNoRuleKind.NAME);
    }
}
