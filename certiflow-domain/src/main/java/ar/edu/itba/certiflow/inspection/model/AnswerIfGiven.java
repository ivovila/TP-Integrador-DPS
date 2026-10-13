package ar.edu.itba.certiflow.inspection.model;

import ar.edu.itba.certiflow.schema.model.Answer;
import java.util.Optional;

final class AnswerIfGiven<A extends Answer> implements AnsweringDescriber<A, Optional<GivenAnswer<A>>> {

    @Override
    public Optional<GivenAnswer<A>> notAnswered() {
        return Optional.empty();
    }

    @Override
    public Optional<GivenAnswer<A>> answered(GivenAnswer<A> givenAnswer) {
        return Optional.of(givenAnswer);
    }
}
