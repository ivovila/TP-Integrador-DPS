package ar.edu.itba.certiflow.certification.model.exceptions;

import ar.edu.itba.certiflow.shared.model.RuleViolationException;

public class RenewalScopeMismatchException extends RuleViolationException {

    public RenewalScopeMismatchException() {
        super("A renewal must keep the scope of the certificate being renewed");
    }
}
