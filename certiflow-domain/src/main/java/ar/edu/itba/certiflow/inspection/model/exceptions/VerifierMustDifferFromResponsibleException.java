package ar.edu.itba.certiflow.inspection.model.exceptions;

import ar.edu.itba.certiflow.shared.model.DomainException;
import ar.edu.itba.certiflow.shared.model.Person;

public class VerifierMustDifferFromResponsibleException extends DomainException {

    public VerifierMustDifferFromResponsibleException(Person verifier) {
        super(verifier.name() + " is responsible for the corrective action and cannot verify it");
    }
}
