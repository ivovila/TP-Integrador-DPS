package ar.edu.itba.certiflow.domain.model.inspection;

import ar.edu.itba.certiflow.domain.model.schema.CriterionId;
import ar.edu.itba.certiflow.domain.rules.CriterionOutcome;

public record CriterionResult(String section, CriterionId criterionId, CriterionOutcome outcome) {
}
