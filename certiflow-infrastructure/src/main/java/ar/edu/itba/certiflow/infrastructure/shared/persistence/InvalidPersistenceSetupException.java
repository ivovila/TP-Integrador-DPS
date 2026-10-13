package ar.edu.itba.certiflow.infrastructure.shared.persistence;

/** La infraestructura se armó con una configuración con la que no puede trabajar. */
public final class InvalidPersistenceSetupException extends RuntimeException {

    public InvalidPersistenceSetupException(String message) {
        super(message);
    }
}
