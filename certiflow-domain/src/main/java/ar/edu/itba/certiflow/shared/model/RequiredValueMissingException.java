package ar.edu.itba.certiflow.shared.model;

public class RequiredValueMissingException extends DomainException {

    public RequiredValueMissingException(String message) {
        super(message);
    }
}
