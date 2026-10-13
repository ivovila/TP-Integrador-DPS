package ar.edu.itba.certiflow.schema.model;

import ar.edu.itba.certiflow.shared.model.DomainException;

public final class AnswerTypeMismatchException extends DomainException {

    public AnswerTypeMismatchException(String expected, RawAnswer received) {
        super("Expected " + expected + " answer but received " + received.text());
    }
}
