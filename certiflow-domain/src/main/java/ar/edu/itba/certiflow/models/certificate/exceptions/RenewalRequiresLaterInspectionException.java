package ar.edu.itba.certiflow.models.certificate.exceptions;

import ar.edu.itba.certiflow.models.certificate.CertificateNumber;
import ar.edu.itba.certiflow.models.shared.DomainException;

public class RenewalRequiresLaterInspectionException extends DomainException {

    public RenewalRequiresLaterInspectionException(CertificateNumber number) {
        super("Certificate " + number.value() + " can only be renewed with an inspection closed after the one it is based on");
    }
}
