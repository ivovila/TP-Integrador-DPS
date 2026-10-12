package ar.edu.itba.certiflow.schema.model;

public record YesNoRule(YesNoAnswer expected) implements ApprovalRule<YesNoAnswer> {

    @Override
    public boolean isSatisfiedBy(YesNoAnswer answer) {
        return expected == answer;
    }

    @Override
    public YesNoAnswer answerFrom(RawAnswer raw) {
        if (raw instanceof RawAnswer.YesNo(boolean value)) {
            return value ? YesNoAnswer.YES : YesNoAnswer.NO;
        }
        throw new AnswerTypeMismatchException("yes/no", raw);
    }
}
