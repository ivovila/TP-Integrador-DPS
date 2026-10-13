package ar.edu.itba.certiflow.inspection.model;

import ar.edu.itba.certiflow.shared.model.Person;
import ar.edu.itba.certiflow.shared.model.Stamp;
import java.time.Instant;

public record Cancellation(Stamp cancelled) implements CancellationState {

    @Override
    public boolean isCancelled() {
        return true;
    }

    @Override
    public <T> T describedBy(CancellationStateDescriber<T> describer) {
        return describer.cancelled(this);
    }

    public Person cancelledBy() {
        return cancelled.by();
    }

    public Instant cancelledAt() {
        return cancelled.at();
    }
}
