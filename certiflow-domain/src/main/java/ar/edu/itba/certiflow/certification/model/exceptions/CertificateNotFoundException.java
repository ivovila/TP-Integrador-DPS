package ar.edu.itba.certiflow.certification.model.exceptions;

import ar.edu.itba.certiflow.certification.model.CertificateNumber;
import ar.edu.itba.certiflow.shared.model.NotFoundException;

public class CertificateNotFoundException extends NotFoundException {

    public CertificateNotFoundException(CertificateNumber certificateNumber) {
        super("Certificate " + certificateNumber.value() + " does not exist");
    }
}
