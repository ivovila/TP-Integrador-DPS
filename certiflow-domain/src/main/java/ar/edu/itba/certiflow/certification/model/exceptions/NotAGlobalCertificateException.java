package ar.edu.itba.certiflow.certification.model.exceptions;

import ar.edu.itba.certiflow.certification.model.CertificateNumber;
import ar.edu.itba.certiflow.shared.model.RuleViolationException;

public class NotAGlobalCertificateException extends RuleViolationException {

    public NotAGlobalCertificateException(CertificateNumber certificateNumber) {
        super("Certificate " + certificateNumber.value() + " is not a global certificate, so it cannot be renewed as one");
    }
}
