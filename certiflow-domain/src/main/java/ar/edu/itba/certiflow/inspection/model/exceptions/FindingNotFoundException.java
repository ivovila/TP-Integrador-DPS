package ar.edu.itba.certiflow.inspection.model.exceptions;

import ar.edu.itba.certiflow.inspection.model.FindingId;
import ar.edu.itba.certiflow.shared.model.NotFoundException;

public class FindingNotFoundException extends NotFoundException {

    public FindingNotFoundException(FindingId id) {
        super("Finding " + id.value() + " does not exist");
    }
}
