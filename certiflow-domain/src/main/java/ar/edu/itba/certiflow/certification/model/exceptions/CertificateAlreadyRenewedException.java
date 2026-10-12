package ar.edu.itba.certiflow.certification.model.exceptions;

import ar.edu.itba.certiflow.certification.model.CertificateNumber;
import ar.edu.itba.certiflow.shared.model.DomainException;

public class CertificateAlreadyRenewedException extends DomainException {

    public CertificateAlreadyRenewedException(CertificateNumber number) {
        super("Certificate " + number.value() + " was already renewed");
    }
}
