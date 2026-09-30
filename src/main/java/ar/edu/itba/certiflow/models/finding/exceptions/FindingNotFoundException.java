package ar.edu.itba.certiflow.models.finding.exceptions;

import ar.edu.itba.certiflow.models.finding.FindingId;
import ar.edu.itba.certiflow.models.shared.DomainException;

public class FindingNotFoundException extends DomainException {

    public FindingNotFoundException(FindingId id) {
        super("Finding " + id.value() + " does not exist");
    }
}
