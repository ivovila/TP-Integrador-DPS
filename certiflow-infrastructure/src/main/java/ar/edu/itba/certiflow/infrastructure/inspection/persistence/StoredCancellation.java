package ar.edu.itba.certiflow.infrastructure.inspection.persistence;

import ar.edu.itba.certiflow.infrastructure.shared.persistence.PersonColumns;
import ar.edu.itba.certiflow.inspection.model.Cancellation;
import ar.edu.itba.certiflow.inspection.model.CancellationState;
import ar.edu.itba.certiflow.inspection.model.NotCancelled;
import ar.edu.itba.certiflow.shared.model.Stamp;
import java.time.Instant;
import java.util.Optional;

/** Las tres columnas de la cancelación de una acción correctiva: quedan vacías mientras no se canceló. */
record StoredCancellation(String cancelledById, String cancelledByName, Instant cancelledAt) {

    CancellationState state() {
        return Optional.ofNullable(cancelledAt)
                .<CancellationState>map(storedInstant -> new Cancellation(
                        new Stamp(new PersonColumns(cancelledById, cancelledByName).toDomain(), storedInstant)))
                .orElseGet(NotCancelled::new);
    }
}
