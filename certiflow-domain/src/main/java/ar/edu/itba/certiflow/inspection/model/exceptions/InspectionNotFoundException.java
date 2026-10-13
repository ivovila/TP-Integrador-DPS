package ar.edu.itba.certiflow.inspection.model.exceptions;

import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.shared.model.NotFoundException;

public class InspectionNotFoundException extends NotFoundException {

    public InspectionNotFoundException(InspectionId id) {
        super("Inspection " + id.value() + " does not exist");
    }
}
