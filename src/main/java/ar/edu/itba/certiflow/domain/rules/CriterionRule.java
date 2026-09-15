package ar.edu.itba.certiflow.domain.rules;

import ar.edu.itba.certiflow.domain.model.shared.Measurement;
import ar.edu.itba.certiflow.domain.model.shared.Response;

public interface CriterionRule {

    CriterionOutcome evaluate(Response response);

    default boolean accepts(Measurement measurement) {
        return false;
    }
}
