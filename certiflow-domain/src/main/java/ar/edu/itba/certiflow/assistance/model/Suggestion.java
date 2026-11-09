package ar.edu.itba.certiflow.assistance.model;

import ar.edu.itba.certiflow.assistance.model.exceptions.SuggestionAlreadyDecidedException;
import ar.edu.itba.certiflow.shared.model.Stamp;

/** Una propuesta que pasó los guardrails, numerada desde 1 dentro de su revisión, con lo que decidió el inspector. */
public record Suggestion(int position, ProposedFinding proposal, SuggestionDecision decision) {

    public boolean isPending() {
        return decision.isPending();
    }

    Suggestion accepted(Stamp accepted) {
        ensurePending();
        return new Suggestion(position, proposal, new Acceptance(accepted));
    }

    Suggestion dismissed(String reason, Stamp dismissed) {
        ensurePending();
        return new Suggestion(position, proposal, new Dismissal(dismissed, reason));
    }

    private void ensurePending() {
        if (!decision.isPending()) {
            throw new SuggestionAlreadyDecidedException(position);
        }
    }
}
