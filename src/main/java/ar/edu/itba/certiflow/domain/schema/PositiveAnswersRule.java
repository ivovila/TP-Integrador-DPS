package ar.edu.itba.certiflow.domain.schema;

import java.util.List;

import ar.edu.itba.certiflow.domain.shared.Answer;
import ar.edu.itba.certiflow.domain.shared.Measurement;
import ar.edu.itba.certiflow.domain.shared.YesNoAnswer;

public record PositiveAnswersRule() implements ApprovalRule {

    @Override
    public CriterionOutcome evaluate(List<Answer> answers, List<Measurement> measurements) {
        List<YesNoAnswer> yesNoAnswers = answers.stream()
                .filter(YesNoAnswer.class::isInstance)
                .map(YesNoAnswer.class::cast)
                .toList();
        if (yesNoAnswers.isEmpty()) {
            return CriterionOutcome.OBSERVED;
        }
        return yesNoAnswers.stream().allMatch(YesNoAnswer::value)
                ? CriterionOutcome.APPROVED
                : CriterionOutcome.REJECTED;
    }
}
