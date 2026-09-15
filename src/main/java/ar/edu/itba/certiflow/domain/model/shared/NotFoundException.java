package ar.edu.itba.certiflow.domain.model.shared;

public class NotFoundException extends DomainException {

    public NotFoundException(String entity, Object id) {
        super("No existe " + entity + " " + id);
    }
}
