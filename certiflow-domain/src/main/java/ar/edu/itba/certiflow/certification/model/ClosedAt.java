package ar.edu.itba.certiflow.certification.model;

import ar.edu.itba.certiflow.inspection.model.InspectionId;
import java.time.Instant;

public record ClosedAt(Instant closedAt) implements InspectionClosure {

    @Override
    public boolean isClosed() {
        return true;
    }

    @Override
    public InspectionReference referenceOf(InspectionId inspectionId) {
        return new InspectionReference(inspectionId, closedAt);
    }
}
