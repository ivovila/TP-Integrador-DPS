package ar.edu.itba.certiflow.schema.model;

public record YesNoRule(YesNoAnswer expected) implements ApprovalRule<YesNoAnswer> {

    @Override
    public boolean isSatisfiedBy(YesNoAnswer answer) {
        return expected == answer;
    }

    @Override
    public YesNoAnswer answerFrom(RawAnswer raw) {
        return raw.as(Boolean.class).map(YesNoAnswer::of)
                .orElseThrow(() -> new AnswerTypeMismatchException("yes/no", raw));
    }
}
