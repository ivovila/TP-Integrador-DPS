package ar.edu.itba.certiflow.models.shared;

public class RequiredValueMissingException extends DomainException {

    public RequiredValueMissingException(String message) {
        super(message);
    }
}
