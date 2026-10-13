package ar.edu.itba.certiflow.infrastructure.inspection.persistence;

import ar.edu.itba.certiflow.shared.model.Stamp;
import ar.edu.itba.certiflow.infrastructure.shared.persistence.PersonColumns;
import ar.edu.itba.certiflow.inspection.model.AttachedEvidence;
import ar.edu.itba.certiflow.inspection.model.CriterionResponse;
import ar.edu.itba.certiflow.inspection.model.GivenAnswer;
import ar.edu.itba.certiflow.inspection.model.Observation;
import ar.edu.itba.certiflow.schema.model.Answer;
import ar.edu.itba.certiflow.schema.model.Criterion;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

final class ResponseRebuilder {

    private ResponseRebuilder() {
    }

    static CriterionResponse<?> rebuild(Criterion<?> criterion, Optional<AnswerColumns> answer, PersonColumns answeredBy,
                                        Instant answeredAt, List<AttachedEvidence> attachments, List<Observation> observations) {
        return typed(criterion, answer, answeredBy, answeredAt, attachments, observations);
    }

    private static <A extends Answer> CriterionResponse<A> typed(Criterion<A> criterion, Optional<AnswerColumns> answer,
                                                                 PersonColumns answeredBy, Instant answeredAt,
                                                                 List<AttachedEvidence> attachments, List<Observation> observations) {
        Optional<GivenAnswer<A>> given = answer.map(columns ->
                new GivenAnswer<>(criterion.answerFrom(columns.toRaw()), new Stamp(answeredBy.toDomain(), answeredAt)));
        return new CriterionResponse<>(criterion, given, attachments, observations);
    }
}
