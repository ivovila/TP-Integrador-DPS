package ar.edu.itba.certiflow.domain.rules;

import ar.edu.itba.certiflow.domain.model.shared.Response;

public record BooleanRule(boolean expected) implements CriterionRule {

    @Override
    public CriterionOutcome evaluate(Response response) {
        return response.answer()
                .map(answer -> answer == expected ? CriterionOutcome.APPROVED : CriterionOutcome.REJECTED)
                .orElse(CriterionOutcome.REJECTED);
    }
}
