package ar.edu.itba.certiflow.certification.model.exceptions;

import ar.edu.itba.certiflow.certification.model.CertificateNumber;
import ar.edu.itba.certiflow.shared.model.DomainException;

public class CertificateNotFoundException extends DomainException {

    public CertificateNotFoundException(CertificateNumber id) {
        super("Certificate " + id.value() + " does not exist");
    }
}
