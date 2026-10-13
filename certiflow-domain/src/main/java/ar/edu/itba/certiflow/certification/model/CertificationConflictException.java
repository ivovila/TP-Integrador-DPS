package ar.edu.itba.certiflow.certification.model;

import ar.edu.itba.certiflow.shared.model.DomainException;

public final class CertificationConflictException extends DomainException {
    public CertificationConflictException(String message) { super(message); }
}
