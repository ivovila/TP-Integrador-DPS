package ar.edu.itba.certiflow.inspection.model;

import ar.edu.itba.certiflow.inspection.model.exceptions.InspectionIncompleteException;
import ar.edu.itba.certiflow.schema.model.Outcome;
import java.util.List;

public record Evaluation(List<CriterionResponse<?>> responses) {

    public Evaluation {
        responses = List.copyOf(responses);
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
