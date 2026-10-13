package ar.edu.itba.certiflow.inspection.model;

public interface CancellationStateDescriber<T> {

    T notCancelled();

    T cancelled(Cancellation cancellation);
}
