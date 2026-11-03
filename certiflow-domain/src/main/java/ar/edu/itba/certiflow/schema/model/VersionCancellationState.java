package ar.edu.itba.certiflow.schema.model;

public interface VersionCancellationState {

    boolean isCancelled();

    <T> T describedBy(VersionCancellationDescriber<T> describer);
}
