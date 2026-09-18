package ar.edu.itba.certiflow.domain.inspection;

import ar.edu.itba.certiflow.domain.evaluation.Outcome;
import ar.edu.itba.certiflow.domain.schema.Criterion;
import ar.edu.itba.certiflow.domain.schema.CriterionNotInSchemaException;
import java.util.List;

/**
 * Fotografía inmutable de las respuestas de una inspección. Los resultados no se almacenan:
 * cada consulta los deriva de la respuesta y de la regla del criterio.
 */
public record Evaluation(List<CriterionResponse<?>> responses) {

    public Evaluation {
        responses = List.copyOf(responses);
    }

    public Outcome outcomeOf(Criterion<?> criterion) {
        return responses.stream()
                .filter(response -> response.isFor(criterion))
                .map(response -> response.outcome())
                .findFirst()
                .orElseThrow(() -> new CriterionNotInSchemaException(criterion));
    }

    public List<CriterionResponse<?>> pending() {
        return withOutcome(Outcome.PENDING);
    }

    public List<CriterionResponse<?>> nonConformities() {
        return responses.stream().filter(response -> response.outcome().raisesFinding()).toList();
    }

    public void ensureComplete() {
        List<CriterionResponse<?>> pending = pending();
        if (!pending.isEmpty()) {
            throw new InspectionIncompleteException(pending.stream().map(response -> response.criterion().code()).toList());
        }
    }

    private List<CriterionResponse<?>> withOutcome(Outcome outcome) {
        return responses.stream().filter(response -> response.outcome() == outcome).toList();
    }
}
