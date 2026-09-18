package ar.edu.itba.certiflow.domain.certificate;

import ar.edu.itba.certiflow.domain.shared.DomainException;

public class CertificateAlreadyRenewedException extends DomainException {

    public CertificateAlreadyRenewedException(CertificateNumber number) {
        super("Certificate " + number.value() + " was already renewed");
    }
}
