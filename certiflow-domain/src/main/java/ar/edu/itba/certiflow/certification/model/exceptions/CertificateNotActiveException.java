package ar.edu.itba.certiflow.certification.model.exceptions;

import ar.edu.itba.certiflow.certification.model.CertificateNumber;
import ar.edu.itba.certiflow.shared.model.RuleViolationException;

public class CertificateNotActiveException extends RuleViolationException {

    public CertificateNotActiveException(CertificateNumber number) {
        super("Certificate " + number.value() + " is not active, so it cannot be suspended");
    }
}
