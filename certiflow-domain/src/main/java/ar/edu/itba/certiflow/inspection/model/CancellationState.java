package ar.edu.itba.certiflow.inspection.model;

/** Si una acción correctiva se canceló, y quién y cuándo lo hizo. */
public interface CancellationState {

    boolean isCancelled();

    <T> T describedBy(CancellationStateDescriber<T> describer);
}
