package ar.edu.itba.certiflow.shared.model;

/** Un servicio del que depende la operación no está disponible: no es un error del pedido ni del estado. */
public abstract class UnavailableException extends DomainException {

    protected UnavailableException(String message) {
        super(message);
    }

    protected UnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
