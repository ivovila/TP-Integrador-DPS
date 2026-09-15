package ar.edu.itba.certiflow.domain.rules;

import java.util.Set;

import ar.edu.itba.certiflow.domain.model.shared.EvidenceType;
import ar.edu.itba.certiflow.domain.model.shared.Response;

public record RequiredEvidenceRule(Set<EvidenceType> required) implements CriterionRule {

    public RequiredEvidenceRule {
        required = Set.copyOf(required);
    }

    @Override
    public CriterionOutcome evaluate(Response response) {
        return response.evidenceTypes().containsAll(required)
                ? CriterionOutcome.APPROVED
                : CriterionOutcome.REJECTED;
    }
}
