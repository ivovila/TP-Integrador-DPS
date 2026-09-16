package ar.edu.itba.certiflow.domain.rule;

import ar.edu.itba.certiflow.domain.Answer;
import ar.edu.itba.certiflow.domain.Checks;
import ar.edu.itba.certiflow.domain.Evaluation;
import ar.edu.itba.certiflow.domain.Outcome;

/** Criterion enforces mandatory evidence. This rule checks the declaration type. */
public record DocumentaryRule() implements EvaluationRule {
    public Evaluation evaluate(Answer answer) {
        Checks.require(answer instanceof Answer.Documentary, "Expected a documentary answer");
        return new Evaluation(
                Outcome.APPROVED, "Documentary declaration and required evidence provided");
    }
}
