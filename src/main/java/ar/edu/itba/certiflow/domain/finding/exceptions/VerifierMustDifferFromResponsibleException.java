package ar.edu.itba.certiflow.domain.finding.exceptions;

import ar.edu.itba.certiflow.domain.shared.DomainException;
import ar.edu.itba.certiflow.domain.shared.Person;

public class VerifierMustDifferFromResponsibleException extends DomainException {

    public VerifierMustDifferFromResponsibleException(Person verifier) {
        super(verifier.name() + " is responsible for the corrective action and cannot verify it");
    }
}
