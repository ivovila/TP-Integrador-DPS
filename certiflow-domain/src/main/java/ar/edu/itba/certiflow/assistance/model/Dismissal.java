package ar.edu.itba.certiflow.assistance.model;

import ar.edu.itba.certiflow.assistance.model.exceptions.DismissalReasonRequiredException;
import ar.edu.itba.certiflow.shared.model.Stamp;

/** El inspector descartó la sugerencia, y dijo por qué: es lo que después explica en qué se equivocó la IA. */
public record Dismissal(Stamp dismissed, String reason) implements SuggestionDecision {

    public Dismissal {
        if (reason.isBlank()) {
            throw new DismissalReasonRequiredException();
        }
    }

    @Override
    public boolean isPending() {
        return false;
    }

    @Override
    public <T> T describedBy(SuggestionDecisionDescriber<T> describer) {
        return describer.dismissed(this);
    }
}
