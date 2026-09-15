package ar.edu.itba.certiflow.domain.rules;

import java.util.List;

import ar.edu.itba.certiflow.domain.model.shared.Response;
import ar.edu.itba.certiflow.domain.model.shared.YesNo;

public record BooleanRule(boolean expected) implements CriterionRule {

    @Override
    public CriterionOutcome evaluate(Response response) {
        List<YesNo> answers = response.all(YesNo.class);
        if (answers.isEmpty()) {
            return CriterionOutcome.REJECTED;
        }
        return answers.stream().allMatch(answer -> answer.value() == expected)
                ? CriterionOutcome.APPROVED
                : CriterionOutcome.REJECTED;
    }
}
