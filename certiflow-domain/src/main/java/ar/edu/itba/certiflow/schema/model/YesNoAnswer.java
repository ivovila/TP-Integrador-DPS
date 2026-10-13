package ar.edu.itba.certiflow.schema.model;

public enum YesNoAnswer implements Answer {
    YES(true),
    NO(false);

    private final boolean value;

    YesNoAnswer(boolean value) {
        this.value = value;
    }

    boolean isWrittenAs(RawAnswer raw) {
        return describedBy(new AnswerText()).equals(raw.text());
    }

    @Override
    public <T> T describedBy(AnswerDescriber<T> describer) {
        return describer.yesNo(value);
    }
}
