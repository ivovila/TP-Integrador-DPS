package ar.edu.itba.certiflow.schema.model;

public interface VersionCancellationDescriber<T> {

    T notCancelled();

    T cancelled(VersionCancellation cancellation);
}
