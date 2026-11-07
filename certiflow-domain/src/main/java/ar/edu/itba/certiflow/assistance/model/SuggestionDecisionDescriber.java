package ar.edu.itba.certiflow.assistance.model;

public interface SuggestionDecisionDescriber<T> {

    T pending();

    T accepted(Acceptance acceptance);

    T dismissed(Dismissal dismissal);
}
