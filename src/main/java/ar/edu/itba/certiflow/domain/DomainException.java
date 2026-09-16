package ar.edu.itba.certiflow.domain;

/** A violated business rule or an invalid business value. */
public final class DomainException extends RuntimeException {
    public DomainException(String message) {
        super(message);
    }
}
