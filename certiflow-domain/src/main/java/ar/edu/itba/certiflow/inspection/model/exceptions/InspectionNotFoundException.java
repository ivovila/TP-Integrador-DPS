package ar.edu.itba.certiflow.inspection.model.exceptions;

import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.shared.model.DomainException;

public class InspectionNotFoundException extends DomainException {

    public InspectionNotFoundException(InspectionId id) {
        super("Inspection " + id.value() + " does not exist");
    }
}
