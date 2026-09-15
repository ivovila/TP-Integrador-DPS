package ar.edu.itba.certiflow.domain.model.schema;

import ar.edu.itba.certiflow.domain.model.shared.Measurement;
import ar.edu.itba.certiflow.domain.model.shared.Response;
import ar.edu.itba.certiflow.domain.rules.CriterionOutcome;
import ar.edu.itba.certiflow.domain.rules.CriterionRule;

public record Criterion(CriterionId id, String description, CriterionRule rule) {

    public boolean accepts(Measurement measurement) {
        return rule.accepts(measurement);
    }

    public CriterionOutcome evaluate(Response response) {
        return rule.evaluate(response);
    }
}
