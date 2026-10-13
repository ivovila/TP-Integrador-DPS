package ar.edu.itba.certiflow.certification.model;

import ar.edu.itba.certiflow.inspection.model.InspectionId;
import java.util.Optional;

public record StillOpen() implements InspectionClosure {

    @Override
    public boolean isClosed() {
        return false;
    }

    @Override
    public Optional<InspectionReference> referenceOf(InspectionId inspectionId) {
        return Optional.empty();
    }
}
