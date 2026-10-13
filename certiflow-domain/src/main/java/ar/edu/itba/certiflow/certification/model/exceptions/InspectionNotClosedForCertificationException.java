package ar.edu.itba.certiflow.certification.model.exceptions;

import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.shared.model.DomainException;

public class InspectionNotClosedForCertificationException extends DomainException {

    public InspectionNotClosedForCertificationException(InspectionId inspectionId) {
        super("Inspection " + inspectionId.value() + " is not closed, so it cannot back a certificate");
    }
}
