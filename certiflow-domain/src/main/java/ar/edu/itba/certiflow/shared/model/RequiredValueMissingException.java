package ar.edu.itba.certiflow.shared.model;

public class RequiredValueMissingException extends InvalidValueException {

    public RequiredValueMissingException(String message) {
        super(message);
    }
}
