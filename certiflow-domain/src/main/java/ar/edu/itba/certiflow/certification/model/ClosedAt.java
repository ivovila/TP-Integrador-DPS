package ar.edu.itba.certiflow.certification.model;

import ar.edu.itba.certiflow.inspection.model.InspectionId;
import java.time.Instant;
import java.util.Optional;

public record ClosedAt(Instant closedAt) implements InspectionClosure {

    @Override
    public boolean isClosed() {
        return true;
    }

    @Override
    public Optional<InspectionReference> referenceOf(InspectionId inspectionId) {
        return Optional.of(new InspectionReference(inspectionId, closedAt));
    }
}
