package ar.edu.itba.certiflow.domain.shared;

public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }
}
