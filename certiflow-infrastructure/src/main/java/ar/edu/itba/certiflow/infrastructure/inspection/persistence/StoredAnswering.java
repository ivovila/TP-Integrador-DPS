package ar.edu.itba.certiflow.infrastructure.inspection.persistence;

import ar.edu.itba.certiflow.infrastructure.shared.persistence.PersonColumns;
import ar.edu.itba.certiflow.inspection.model.Answering;
import ar.edu.itba.certiflow.inspection.model.GivenAnswer;
import ar.edu.itba.certiflow.inspection.model.NotAnswered;
import ar.edu.itba.certiflow.schema.model.Answer;
import ar.edu.itba.certiflow.schema.model.Criterion;
import ar.edu.itba.certiflow.schema.model.RawAnswer;
import ar.edu.itba.certiflow.shared.model.Stamp;
import java.time.Instant;
import java.util.Optional;

/** Las cinco columnas de la respuesta de un criterio: quedan vacías mientras no se respondió. */
record StoredAnswering(String answerType, String answerValue, String answeredById, String answeredByName,
                       Instant answeredAt) {

    <A extends Answer> Answering<A> answeringOf(Criterion<A> criterion) {
        return Optional.ofNullable(answerType)
                .<Answering<A>>map(storedType -> new GivenAnswer<>(criterion.answerFrom(new RawAnswer(answerValue)),
                        new Stamp(new PersonColumns(answeredById, answeredByName).toDomain(), answeredAt)))
                .orElseGet(NotAnswered::new);
    }
}
