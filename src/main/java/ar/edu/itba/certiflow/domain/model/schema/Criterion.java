package ar.edu.itba.certiflow.domain.model.schema;

import ar.edu.itba.certiflow.domain.model.shared.Measurement;
import ar.edu.itba.certiflow.domain.model.shared.Response;
import ar.edu.itba.certiflow.domain.rules.CriterionOutcome;
import ar.edu.itba.certiflow.domain.rules.CriterionRule;
import ar.edu.itba.certiflow.domain.rules.MeasurementRule;

public record Criterion(CriterionId id, String description, CriterionRule rule) {

    public boolean accepts(Measurement measurement) {
        return rule instanceof MeasurementRule measurementRule && measurementRule.accepts(measurement);
    }

    public CriterionOutcome evaluate(Response response) {
        return rule.evaluate(response);
    }
}
