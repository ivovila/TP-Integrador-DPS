package ar.edu.itba.certiflow.certification.model.exceptions;

import ar.edu.itba.certiflow.shared.model.DomainException;

public class RenewalScopeMismatchException extends DomainException {

    public RenewalScopeMismatchException() {
        super("A renewal must keep the scope of the certificate being renewed");
    }
}
