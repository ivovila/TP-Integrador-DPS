package ar.edu.itba.certiflow.inspection.model;

public record NotCancelled() implements CancellationState {

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public <T> T describedBy(CancellationStateDescriber<T> describer) {
        return describer.notCancelled();
    }
}
