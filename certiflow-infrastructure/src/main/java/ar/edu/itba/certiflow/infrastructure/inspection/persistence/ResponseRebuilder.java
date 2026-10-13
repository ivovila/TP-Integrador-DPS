package ar.edu.itba.certiflow.infrastructure.inspection.persistence;

import ar.edu.itba.certiflow.inspection.model.AttachedEvidence;
import ar.edu.itba.certiflow.inspection.model.CriterionResponse;
import ar.edu.itba.certiflow.inspection.model.Observation;
import ar.edu.itba.certiflow.schema.model.Answer;
import ar.edu.itba.certiflow.schema.model.Criterion;
import java.util.List;

final class ResponseRebuilder {

    private ResponseRebuilder() {
    }

    static <A extends Answer> CriterionResponse<A> rebuild(Criterion<A> criterion, StoredAnswering storedAnswering,
                                                           List<AttachedEvidence> attachments,
                                                           List<Observation> observations) {
        return new CriterionResponse<>(criterion, storedAnswering.answeringOf(criterion), attachments, observations);
    }
}
