package ar.edu.itba.certiflow.assistance.model;

import ar.edu.itba.certiflow.shared.model.Stamp;

/**
 * El inspector aceptó la sugerencia. No cambia la inspección: para que haya hallazgo, corrige la respuesta del
 * criterio y la regla lo levanta al cerrar.
 */
public record Acceptance(Stamp accepted) implements SuggestionDecision {

    @Override
    public boolean isPending() {
        return false;
    }

    @Override
    public <T> T describedBy(SuggestionDecisionDescriber<T> describer) {
        return describer.accepted(this);
    }
}
