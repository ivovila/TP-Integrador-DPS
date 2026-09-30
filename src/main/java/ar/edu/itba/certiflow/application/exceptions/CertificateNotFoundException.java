package ar.edu.itba.certiflow.application.exceptions;

import ar.edu.itba.certiflow.models.certificate.CertificateNumber;
import ar.edu.itba.certiflow.models.shared.DomainException;

public class CertificateNotFoundException extends DomainException {

    public CertificateNotFoundException(CertificateNumber id) {
        super("Certificate " + id.value() + " does not exist");
    }
}
