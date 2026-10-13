package ar.edu.itba.certiflow.infrastructure.inspection.persistence;

import ar.edu.itba.certiflow.infrastructure.shared.persistence.PersonColumns;
import ar.edu.itba.certiflow.inspection.model.AnsweringDescriber;
import ar.edu.itba.certiflow.inspection.model.GivenAnswer;
import ar.edu.itba.certiflow.schema.model.Answer;

final class StoredAnsweringDescriber<A extends Answer> implements AnsweringDescriber<A, StoredAnswering> {

    @Override
    public StoredAnswering notAnswered() {
        return new StoredAnswering(null, null, null, null, null);
    }

    @Override
    public StoredAnswering answered(GivenAnswer<A> givenAnswer) {
        AnswerColumns answer = givenAnswer.value().describedBy(new AnswerColumnsDescriber());
        PersonColumns answeredBy = new PersonColumns(givenAnswer.answeredBy());
        return new StoredAnswering(answer.type(), answer.value(), answeredBy.id(), answeredBy.name(),
                givenAnswer.answeredAt());
    }
}
