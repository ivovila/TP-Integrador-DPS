package ar.edu.itba.certiflow.schema.model;

import java.util.Arrays;

public enum YesNoAnswer implements Answer {
    YES(true),
    NO(false);

    private final boolean value;

    YesNoAnswer(boolean value) {
        this.value = value;
    }

    public static YesNoAnswer of(boolean value) {
        return Arrays.stream(values()).filter(answer -> answer.value == value).findFirst().orElseThrow();
    }

    @Override
    public RawAnswer raw() {
        return new RawAnswer(value);
    }
}
