package ar.edu.itba.certiflow.domain.rule;

import ar.edu.itba.certiflow.domain.Answer;
import ar.edu.itba.certiflow.domain.Checks;
import ar.edu.itba.certiflow.domain.Evaluation;
import ar.edu.itba.certiflow.domain.Outcome;

public record BooleanRule(boolean expected, Outcome mismatch) implements EvaluationRule {
    public BooleanRule {
        Checks.require(
                mismatch == Outcome.OBSERVED || mismatch == Outcome.REJECTED,
                "Invalid mismatch outcome");
    }

    public Evaluation evaluate(Answer answer) {
        Checks.require(answer instanceof Answer.YesNo, "Expected a yes/no answer");
        boolean actual = ((Answer.YesNo) answer).value();
        return new Evaluation(
                actual == expected ? Outcome.APPROVED : mismatch,
                "Expected " + expected + ", answered " + actual);
    }
}
