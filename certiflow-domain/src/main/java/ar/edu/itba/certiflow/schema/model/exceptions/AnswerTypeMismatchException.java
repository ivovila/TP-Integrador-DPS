package ar.edu.itba.certiflow.schema.model.exceptions;

import ar.edu.itba.certiflow.schema.model.RawAnswer;
import ar.edu.itba.certiflow.shared.model.InvalidValueException;

public final class AnswerTypeMismatchException extends InvalidValueException {

    public AnswerTypeMismatchException(String expected, RawAnswer received) {
        super("Expected " + expected + " answer but received " + received.text());
    }
}
