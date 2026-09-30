package ar.edu.itba.certiflow.models.inspection.exceptions;

import ar.edu.itba.certiflow.models.inspection.InspectionId;
import ar.edu.itba.certiflow.models.shared.DomainException;

public class InspectionNotFoundException extends DomainException {

    public InspectionNotFoundException(InspectionId id) {
        super("Inspection " + id.value() + " does not exist");
    }
}
