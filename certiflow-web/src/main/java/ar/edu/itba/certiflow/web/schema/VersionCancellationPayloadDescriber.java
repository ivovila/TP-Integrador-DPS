package ar.edu.itba.certiflow.web.schema;

import ar.edu.itba.certiflow.schema.model.VersionCancellation;
import ar.edu.itba.certiflow.schema.model.VersionCancellationDescriber;
import ar.edu.itba.certiflow.web.support.PersonPayload;
import java.util.Optional;

final class VersionCancellationPayloadDescriber implements VersionCancellationDescriber<Optional<VersionCancellationView>> {

    @Override
    public Optional<VersionCancellationView> notCancelled() {
        return Optional.empty();
    }

    @Override
    public Optional<VersionCancellationView> cancelled(VersionCancellation cancellation) {
        return Optional.of(new VersionCancellationView(PersonPayload.of(cancellation.cancelled().author()),
                cancellation.cancelled().instant(), cancellation.reason()));
    }
}
