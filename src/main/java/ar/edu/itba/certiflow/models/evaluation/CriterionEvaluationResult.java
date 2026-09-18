package ar.edu.itba.certiflow.models.evaluation;

public record CriterionEvaluationResult<T>(
        Criterion<T> criterion,
        InspectorFeedback<T> inspectorFeedback,
        Outcome outcome) {
}
