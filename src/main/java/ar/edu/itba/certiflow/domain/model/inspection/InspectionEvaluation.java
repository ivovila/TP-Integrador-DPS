package ar.edu.itba.certiflow.domain.model.inspection;

import java.util.List;

import ar.edu.itba.certiflow.domain.model.schema.CriterionId;
import ar.edu.itba.certiflow.domain.rules.CriterionOutcome;

public record InspectionEvaluation(List<CriterionResult> results) {

    public InspectionEvaluation {
        results = List.copyOf(results);
    }

    public CriterionOutcome outcomeOf(CriterionId criterionId) {
        return results.stream()
                .filter(result -> result.criterionId().equals(criterionId))
                .map(CriterionResult::outcome)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("El criterio no fue evaluado en esta inspeccion"));
    }

    public CriterionOutcome sectionOutcome(String section) {
        return results.stream()
                .filter(result -> result.section().equals(section))
                .map(CriterionResult::outcome)
                .reduce(CriterionOutcome.APPROVED, CriterionOutcome::worst);
    }

    public CriterionOutcome overall() {
        return results.stream()
                .map(CriterionResult::outcome)
                .reduce(CriterionOutcome.APPROVED, CriterionOutcome::worst);
    }

    public List<CriterionId> withOutcome(CriterionOutcome outcome) {
        return results.stream()
                .filter(result -> result.outcome() == outcome)
                .map(CriterionResult::criterionId)
                .toList();
    }
}
