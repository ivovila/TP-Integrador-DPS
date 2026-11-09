package ar.edu.itba.certiflow.assistance.model.exceptions;

import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.shared.model.RuleViolationException;

public class ReviewOfClosedInspectionException extends RuleViolationException {

    public ReviewOfClosedInspectionException(InspectionId inspectionId) {
        super("Inspection " + inspectionId.value() + " is closed: the assistant only reviews open inspections");
    }
}
