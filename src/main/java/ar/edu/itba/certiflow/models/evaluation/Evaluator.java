package ar.edu.itba.certiflow.models.evaluation;

@FunctionalInterface
public interface Evaluator<T> {
    Outcome evaluate(InspectorFeedback<T> inspectorFeedback);
}
