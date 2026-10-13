package ar.edu.itba.certiflow.inspection.model;

import ar.edu.itba.certiflow.schema.model.Answer;

public interface AnsweringDescriber<A extends Answer, T> {

    T notAnswered();

    T answered(GivenAnswer<A> givenAnswer);
}
