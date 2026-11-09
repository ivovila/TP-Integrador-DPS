package ar.edu.itba.certiflow.assistance.model;

/** Lo que decidió el inspector sobre una sugerencia: todavía nada, aceptarla o descartarla. */
public interface SuggestionDecision {

    boolean isPending();

    <T> T describedBy(SuggestionDecisionDescriber<T> describer);
}
