package ar.edu.itba.certiflow.assistance.model.exceptions;

import ar.edu.itba.certiflow.shared.model.UnavailableException;

/** El modelo no respondió, o respondió algo que no cumple el formato que el asistente promete. */
public class AssistantUnavailableException extends UnavailableException {

    public AssistantUnavailableException(String message) {
        super(message);
    }

    public AssistantUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
