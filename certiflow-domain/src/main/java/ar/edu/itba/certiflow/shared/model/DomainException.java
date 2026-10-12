package ar.edu.itba.certiflow.shared.model;

public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }
}
