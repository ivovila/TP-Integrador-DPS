package ar.edu.itba.certiflow.domain.rules;

import java.util.Collections;
import java.util.Set;

import ar.edu.itba.certiflow.domain.model.shared.Response;

public record EnumOptionRule(Set<String> approvedOptions, Set<String> observedOptions) implements CriterionRule {

    public EnumOptionRule {
        approvedOptions = Set.copyOf(approvedOptions);
        observedOptions = Set.copyOf(observedOptions);
        if (approvedOptions.isEmpty()) {
            throw new IllegalArgumentException("Debe existir al menos una opcion aprobada");
        }
        if (!Collections.disjoint(approvedOptions, observedOptions)) {
            throw new IllegalArgumentException("Una opcion no puede ser aprobada y observada a la vez");
        }
    }

    @Override
    public CriterionOutcome evaluate(Response response) {
        return response.option()
                .map(this::classify)
                .orElse(CriterionOutcome.REJECTED);
    }

    private CriterionOutcome classify(String option) {
        if (approvedOptions.contains(option)) {
            return CriterionOutcome.APPROVED;
        }
        if (observedOptions.contains(option)) {
            return CriterionOutcome.OBSERVED;
        }
        return CriterionOutcome.REJECTED;
    }
}
