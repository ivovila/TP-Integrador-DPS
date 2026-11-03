package ar.edu.itba.certiflow.schema.model;

import ar.edu.itba.certiflow.shared.model.RequiredValueMissingException;
import ar.edu.itba.certiflow.shared.model.Stamp;

public record VersionCancellation(Stamp cancelled, String reason) implements VersionCancellationState {

    public VersionCancellation {
        if (reason.isBlank()) {
            throw new RequiredValueMissingException("A version cancellation must state its reason");
        }
    }

    @Override
    public boolean isCancelled() {
        return true;
    }

    @Override
    public <T> T describedBy(VersionCancellationDescriber<T> describer) {
        return describer.cancelled(this);
    }
}
