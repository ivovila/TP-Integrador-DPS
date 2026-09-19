package ar.edu.itba.certiflow.models.finding.exceptions;

import ar.edu.itba.certiflow.models.shared.DomainException;
import ar.edu.itba.certiflow.models.shared.Person;

public class VerifierMustDifferFromResponsibleException extends DomainException {

    public VerifierMustDifferFromResponsibleException(Person verifier) {
        super(verifier.name() + " is responsible for the corrective action and cannot verify it");
    }
}
