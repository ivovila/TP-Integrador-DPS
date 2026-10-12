package ar.edu.itba.certiflow.inspection.model.exceptions;

import ar.edu.itba.certiflow.shared.model.DomainException;

public class FindingAlreadyClosedException extends DomainException {

    public FindingAlreadyClosedException() {
        super("A closed finding accepts no further corrective actions or verifications");
    }
}
