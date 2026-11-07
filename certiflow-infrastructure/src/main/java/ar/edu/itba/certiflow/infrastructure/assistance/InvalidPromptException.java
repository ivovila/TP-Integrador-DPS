package ar.edu.itba.certiflow.infrastructure.assistance;

/** La versión de prompt configurada no está publicada, o su texto ya no es el que se publicó. */
public final class InvalidPromptException extends RuntimeException {

    public InvalidPromptException(String message) {
        super(message);
    }

    public InvalidPromptException(String message, Throwable cause) {
        super(message, cause);
    }
}
