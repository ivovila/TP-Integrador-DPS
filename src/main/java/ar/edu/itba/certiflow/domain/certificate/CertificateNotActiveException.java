package ar.edu.itba.certiflow.domain.certificate;

import ar.edu.itba.certiflow.domain.shared.DomainException;

public class CertificateNotActiveException extends DomainException {

    public CertificateNotActiveException(CertificateNumber number) {
        super("Certificate " + number.value() + " was already suspended or renewed, so it cannot be suspended");
    }
}
