package ar.edu.itba.certiflow.domain.finding.exceptions;

import ar.edu.itba.certiflow.domain.shared.DomainException;

public class FindingAlreadyClosedException extends DomainException {

    public FindingAlreadyClosedException() {
        super("A closed finding accepts no further corrective actions or verifications");
    }
}
