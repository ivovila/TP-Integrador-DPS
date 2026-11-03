package ar.edu.itba.certiflow.infrastructure.schema.persistence;

import ar.edu.itba.certiflow.infrastructure.shared.persistence.PersonColumns;
import ar.edu.itba.certiflow.schema.model.VersionCancellation;
import ar.edu.itba.certiflow.schema.model.VersionCancellationDescriber;

final class StoredVersionCancellationDescriber implements VersionCancellationDescriber<StoredVersionCancellation> {

    @Override
    public StoredVersionCancellation notCancelled() {
        return new StoredVersionCancellation(null, null, null, null);
    }

    @Override
    public StoredVersionCancellation cancelled(VersionCancellation cancellation) {
        PersonColumns cancelledBy = new PersonColumns(cancellation.cancelled().author());
        return new StoredVersionCancellation(cancelledBy.id(), cancelledBy.name(), cancellation.cancelled().instant(),
                cancellation.reason());
    }
}
