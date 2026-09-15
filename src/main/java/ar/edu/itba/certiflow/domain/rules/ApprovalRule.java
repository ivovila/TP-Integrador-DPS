package ar.edu.itba.certiflow.domain.rules;

import java.util.List;

import ar.edu.itba.certiflow.domain.model.shared.Answer;
import ar.edu.itba.certiflow.domain.model.shared.Measurement;

public interface ApprovalRule {
    CriterionOutcome evaluate(List<Answer> answers, List<Measurement> measurements);

    default boolean accepts(Measurement measurement) {
        return false;
    }
}
