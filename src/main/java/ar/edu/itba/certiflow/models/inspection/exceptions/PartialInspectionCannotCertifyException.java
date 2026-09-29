package ar.edu.itba.certiflow.models.inspection.exceptions;

import ar.edu.itba.certiflow.models.shared.DomainException;

public class PartialInspectionCannotCertifyException extends DomainException {

    public PartialInspectionCannotCertifyException() {
        super("Only an inspection that covers the whole schema can back a certificate");
    }
}
