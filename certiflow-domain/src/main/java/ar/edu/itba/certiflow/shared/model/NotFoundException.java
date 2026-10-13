package ar.edu.itba.certiflow.shared.model;

/** Algo que el pedido nombra no existe. */
public abstract class NotFoundException extends DomainException {

    protected NotFoundException(String message) {
        super(message);
    }
}
