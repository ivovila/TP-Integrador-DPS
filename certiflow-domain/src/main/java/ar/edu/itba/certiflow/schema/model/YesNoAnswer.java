package ar.edu.itba.certiflow.schema.model;


public enum YesNoAnswer implements Answer {
    YES,
    NO;

    @Override
    public RawAnswer raw() {
        return new RawAnswer.YesNo(this == YES);
    }
}
