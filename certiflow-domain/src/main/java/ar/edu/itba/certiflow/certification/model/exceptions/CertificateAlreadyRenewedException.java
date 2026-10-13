package ar.edu.itba.certiflow.certification.model.exceptions;

import ar.edu.itba.certiflow.certification.model.CertificateNumber;
import ar.edu.itba.certiflow.shared.model.RuleViolationException;

public class CertificateAlreadyRenewedException extends RuleViolationException {

    public CertificateAlreadyRenewedException(CertificateNumber number) {
        super("Certificate " + number.value() + " was already renewed");
    }
}
