package ar.edu.itba.certiflow.inspection.model.exceptions;

import ar.edu.itba.certiflow.inspection.model.FindingId;
import ar.edu.itba.certiflow.shared.model.DomainException;

public class FindingNotFoundException extends DomainException {

    public FindingNotFoundException(FindingId id) {
        super("Finding " + id.value() + " does not exist");
    }
}
