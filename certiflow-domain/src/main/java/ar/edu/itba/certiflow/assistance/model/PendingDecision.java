package ar.edu.itba.certiflow.assistance.model;

public record PendingDecision() implements SuggestionDecision {

    @Override
    public boolean isPending() {
        return true;
    }

    @Override
    public <T> T describedBy(SuggestionDecisionDescriber<T> describer) {
        return describer.pending();
    }
}
