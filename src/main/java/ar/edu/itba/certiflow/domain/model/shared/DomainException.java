package ar.edu.itba.certiflow.domain.model.shared;

public class DomainException extends RuntimeException {

    public DomainException(String message) {
        super(message);
    }
}
