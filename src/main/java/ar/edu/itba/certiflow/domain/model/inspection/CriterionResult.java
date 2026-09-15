package ar.edu.itba.certiflow.domain.model.inspection;

import java.util.List;

import ar.edu.itba.certiflow.domain.model.schema.CriterionId;
import ar.edu.itba.certiflow.domain.model.shared.Evidence;
import ar.edu.itba.certiflow.domain.rules.CriterionOutcome;

public record CriterionResult(String section, CriterionId criterionId, CriterionOutcome outcome,
                              List<Evidence> evidences) {

    public CriterionResult {
        evidences = List.copyOf(evidences);
    }
}
