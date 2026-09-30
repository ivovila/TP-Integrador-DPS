package ar.edu.itba.certiflow.models.finding.exceptions;

import ar.edu.itba.certiflow.models.shared.DomainException;

public class FindingAlreadyClosedException extends DomainException {

    public FindingAlreadyClosedException() {
        super("A closed finding accepts no further corrective actions or verifications");
    }
}
