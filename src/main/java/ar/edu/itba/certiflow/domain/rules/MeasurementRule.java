package ar.edu.itba.certiflow.domain.rules;

import ar.edu.itba.certiflow.domain.model.shared.Measurement;

public interface MeasurementRule extends CriterionRule {

    boolean accepts(Measurement measurement);
}
