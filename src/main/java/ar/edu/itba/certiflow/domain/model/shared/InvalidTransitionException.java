package ar.edu.itba.certiflow.domain.model.shared;

public class InvalidTransitionException extends DomainException {

    public InvalidTransitionException(String message) {
        super(message);
    }
}
