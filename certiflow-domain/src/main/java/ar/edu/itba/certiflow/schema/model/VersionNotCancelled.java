package ar.edu.itba.certiflow.schema.model;

public record VersionNotCancelled() implements VersionCancellationState {

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public <T> T describedBy(VersionCancellationDescriber<T> describer) {
        return describer.notCancelled();
    }
}
