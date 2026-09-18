package ar.edu.itba.certiflow.models.evaluation;

import ar.edu.itba.certiflow.models.finding.CorrectiveActionPolicy;
import ar.edu.itba.certiflow.models.finding.FindingPolicy;
import java.util.UUID;

public record Criterion<T>(
        UUID id,
        String description,
        Evaluator<T> evaluator,
        FindingPolicy findingPolicy) {
    public CriterionEvaluationResult<T> evaluate(InspectorFeedback<T> inspectorFeedback) {
        return new CriterionEvaluationResult<>(this, inspectorFeedback, evaluator.evaluate(inspectorFeedback));
    }
}
