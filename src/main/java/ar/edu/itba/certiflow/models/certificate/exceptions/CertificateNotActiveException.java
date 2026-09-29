package ar.edu.itba.certiflow.models.certificate.exceptions;

import ar.edu.itba.certiflow.models.certificate.CertificateNumber;
import ar.edu.itba.certiflow.models.shared.DomainException;

public class CertificateNotActiveException extends DomainException {

    public CertificateNotActiveException(CertificateNumber number) {
        super("Certificate " + number.value() + " is not active, so it cannot be suspended");
    }
}
