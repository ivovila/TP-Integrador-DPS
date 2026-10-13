package ar.edu.itba.certiflow.certification.model.exceptions;

import ar.edu.itba.certiflow.certification.model.CertificateNumber;
import ar.edu.itba.certiflow.shared.model.RuleViolationException;

public class RenewalRequiresLaterInspectionException extends RuleViolationException {

    public RenewalRequiresLaterInspectionException(CertificateNumber number) {
        super("Certificate " + number.value() + " can only be renewed with an inspection closed after the one it is based on");
    }
}
