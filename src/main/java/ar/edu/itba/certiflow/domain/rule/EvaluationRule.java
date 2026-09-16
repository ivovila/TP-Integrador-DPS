package ar.edu.itba.certiflow.domain.rule;

import ar.edu.itba.certiflow.domain.Answer;
import ar.edu.itba.certiflow.domain.Evaluation;

/**
 * Implementations must be immutable and deterministic. Invalid answer types must fail explicitly.
 */
public interface EvaluationRule {
    Evaluation evaluate(Answer answer);
}
