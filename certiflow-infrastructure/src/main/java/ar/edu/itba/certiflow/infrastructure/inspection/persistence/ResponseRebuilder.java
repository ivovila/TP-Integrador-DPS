package ar.edu.itba.certiflow.infrastructure.inspection.persistence;

import ar.edu.itba.certiflow.infrastructure.shared.persistence.PersonColumns;
import ar.edu.itba.certiflow.inspection.model.AttachedEvidence;
import ar.edu.itba.certiflow.inspection.model.CriterionResponse;
import ar.edu.itba.certiflow.inspection.model.GivenAnswer;
import ar.edu.itba.certiflow.inspection.model.Observation;
import ar.edu.itba.certiflow.schema.model.Answer;
import ar.edu.itba.certiflow.schema.model.Criterion;
import ar.edu.itba.certiflow.schema.model.RawAnswer;
import ar.edu.itba.certiflow.shared.model.Stamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

final class ResponseRebuilder {

    private ResponseRebuilder() {
    }

    /** El tipo y el valor de la respuesta faltan mientras el criterio no se respondió. */
    static CriterionResponse<?> rebuild(Criterion<?> criterion, String answerType, String answerValue, PersonColumns answeredBy,
                                        Instant answeredAt, List<AttachedEvidence> attachments, List<Observation> observations) {
        return Optional.ofNullable(answerType)
                .<CriterionResponse<?>>map(type -> answered(criterion, new AnswerColumns(type, answerValue),
                        new Stamp(answeredBy.toDomain(), answeredAt), attachments, observations))
                .orElseGet(() -> new CriterionResponse<>(criterion, null, attachments, observations));
    }

    private static <A extends Answer> CriterionResponse<A> answered(Criterion<A> criterion, AnswerColumns answer, Stamp answered,
                                                                    List<AttachedEvidence> attachments, List<Observation> observations) {
        return new CriterionResponse<>(criterion, new GivenAnswer<>(criterion.answerFrom(new RawAnswer(answer.value())), answered), attachments,
                observations);
    }
}
