package ar.edu.itba.certiflow.shared.model;

public class InvalidPageRequestException extends InvalidValueException {

    public InvalidPageRequestException(int number, int size) {
        super("Page must be 0 or more and size 1 or more, but page " + number + " of size " + size + " was requested");
    }
}
