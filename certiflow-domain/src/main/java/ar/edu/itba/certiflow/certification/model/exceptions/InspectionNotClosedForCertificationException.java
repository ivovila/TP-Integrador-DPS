package ar.edu.itba.certiflow.certification.model.exceptions;

import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.shared.model.RuleViolationException;

public class InspectionNotClosedForCertificationException extends RuleViolationException {

    public InspectionNotClosedForCertificationException(InspectionId inspectionId) {
        super("Inspection " + inspectionId.value() + " is not closed, so it cannot back a certificate");
    }
}
