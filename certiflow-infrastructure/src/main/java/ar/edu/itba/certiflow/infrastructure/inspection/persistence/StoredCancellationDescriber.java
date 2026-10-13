package ar.edu.itba.certiflow.infrastructure.inspection.persistence;

import ar.edu.itba.certiflow.infrastructure.shared.persistence.PersonColumns;
import ar.edu.itba.certiflow.inspection.model.Cancellation;
import ar.edu.itba.certiflow.inspection.model.CancellationStateDescriber;

final class StoredCancellationDescriber implements CancellationStateDescriber<StoredCancellation> {

    @Override
    public StoredCancellation notCancelled() {
        return new StoredCancellation(null, null, null);
    }

    @Override
    public StoredCancellation cancelled(Cancellation cancellation) {
        PersonColumns cancelledBy = new PersonColumns(cancellation.cancelledBy());
        return new StoredCancellation(cancelledBy.id(), cancelledBy.name(), cancellation.cancelledAt());
    }
}
