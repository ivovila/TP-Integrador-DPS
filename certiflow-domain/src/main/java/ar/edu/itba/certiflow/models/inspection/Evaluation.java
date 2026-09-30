package ar.edu.itba.certiflow.models.inspection;

import ar.edu.itba.certiflow.models.evaluation.Outcome;
import ar.edu.itba.certiflow.models.inspection.exceptions.InspectionIncompleteException;
import ar.edu.itba.certiflow.models.schema.Criterion;
import ar.edu.itba.certiflow.models.schema.exceptions.CriterionNotInSchemaException;
import java.util.List;

public record Evaluation(List<CriterionResponse<?>> responses) {

    public Evaluation {
        responses = List.copyOf(responses);
    }

    public Outcome outcomeOf(Criterion<?> criterion) {
        return responses.stream()
                .filter(response -> response.isFor(criterion))
                .map(CriterionResponse::outcome)
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
        List<CriterionResponse<?>> pendingResponses = pending();
        if (!pendingResponses.isEmpty()) {
            throw new InspectionIncompleteException(pendingResponses.stream().map(response -> response.criterion().code()).toList());
        }
    }

    private List<CriterionResponse<?>> withOutcome(Outcome outcome) {
        return responses.stream().filter(response -> response.outcome() == outcome).toList();
    }
}
