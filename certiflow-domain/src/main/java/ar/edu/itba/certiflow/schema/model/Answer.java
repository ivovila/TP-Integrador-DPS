package ar.edu.itba.certiflow.schema.model;

public interface Answer {

    <T> T describedBy(AnswerDescriber<T> describer);
}
