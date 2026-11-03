package ar.edu.itba.certiflow.infrastructure.schema.persistence;

import ar.edu.itba.certiflow.infrastructure.shared.persistence.PersonColumns;
import ar.edu.itba.certiflow.schema.model.VersionCancellation;
import ar.edu.itba.certiflow.schema.model.VersionCancellationState;
import ar.edu.itba.certiflow.schema.model.VersionNotCancelled;
import ar.edu.itba.certiflow.shared.model.Stamp;
import java.time.Instant;
import java.util.Optional;

record StoredVersionCancellation(String cancelledById, String cancelledByName, Instant cancelledAt, String reason) {

    VersionCancellationState state() {
        return Optional.ofNullable(cancelledAt)
                .<VersionCancellationState>map(storedInstant -> new VersionCancellation(
                        new Stamp(new PersonColumns(cancelledById, cancelledByName).toDomain(), storedInstant), reason))
                .orElseGet(VersionNotCancelled::new);
    }
}
