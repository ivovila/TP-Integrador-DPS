package ar.edu.itba.certiflow.models.evaluation;

public record EvaluationRequest<T>(Criterion<T> criterion, InspectorFeedback<T> inspectorFeedback) {
    public CriterionEvaluationResult<T> evaluate() {
        return criterion.evaluate(inspectorFeedback);
    }
}
