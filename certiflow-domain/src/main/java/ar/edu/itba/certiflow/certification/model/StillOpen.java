package ar.edu.itba.certiflow.certification.model;

import ar.edu.itba.certiflow.certification.model.exceptions.InspectionNotClosedForCertificationException;
import ar.edu.itba.certiflow.inspection.model.InspectionId;

public record StillOpen() implements InspectionClosure {

    @Override
    public boolean isClosed() {
        return false;
    }

    @Override
    public InspectionReference referenceOf(InspectionId inspectionId) {
        throw new InspectionNotClosedForCertificationException(inspectionId);
    }
}
