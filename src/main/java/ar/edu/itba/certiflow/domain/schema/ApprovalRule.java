package ar.edu.itba.certiflow.domain.schema;

import java.util.List;

import ar.edu.itba.certiflow.domain.shared.Answer;
import ar.edu.itba.certiflow.domain.shared.Measurement;

public interface ApprovalRule {
    CriterionOutcome evaluate(List<Answer> answers, List<Measurement> measurements);

    default boolean accepts(Measurement measurement) {
        return false;
    }
}
