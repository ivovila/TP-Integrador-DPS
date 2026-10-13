package ar.edu.itba.certiflow.inspection.model.exceptions;

import ar.edu.itba.certiflow.shared.model.RuleViolationException;
import ar.edu.itba.certiflow.shared.model.Person;

public class VerifierMustDifferFromResponsibleException extends RuleViolationException {

    public VerifierMustDifferFromResponsibleException(Person verifier) {
        super(verifier.name() + " is responsible for the corrective action and cannot verify it");
    }
}
