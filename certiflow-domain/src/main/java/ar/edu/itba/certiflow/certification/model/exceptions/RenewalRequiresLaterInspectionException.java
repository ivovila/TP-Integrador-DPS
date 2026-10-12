package ar.edu.itba.certiflow.certification.model.exceptions;

import ar.edu.itba.certiflow.certification.model.CertificateNumber;
import ar.edu.itba.certiflow.shared.model.DomainException;

public class RenewalRequiresLaterInspectionException extends DomainException {

    public RenewalRequiresLaterInspectionException(CertificateNumber number) {
        super("Certificate " + number.value() + " can only be renewed with an inspection closed after the one it is based on");
    }
}
