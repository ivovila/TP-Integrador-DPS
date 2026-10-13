package ar.edu.itba.certiflow.infrastructure.shared.persistence;

/** Un dato guardado que este código no sabe leer: una etiqueta o un código que nadie reconoce. */
public final class UnreadableStoredDataException extends RuntimeException {

    public UnreadableStoredDataException(String what, String storedValue) {
        super("Stored " + what + " is not recognised: " + storedValue);
    }
}
