package ar.edu.itba.certiflow.models.certificate.exceptions;

import ar.edu.itba.certiflow.models.certificate.CertificateNumber;
import ar.edu.itba.certiflow.models.shared.DomainException;

public class CertificateAlreadyRenewedException extends DomainException {

    public CertificateAlreadyRenewedException(CertificateNumber number) {
        super("Certificate " + number.value() + " was already renewed");
    }
}
