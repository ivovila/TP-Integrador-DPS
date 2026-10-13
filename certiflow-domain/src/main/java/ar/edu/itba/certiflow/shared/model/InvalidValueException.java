package ar.edu.itba.certiflow.shared.model;

/** Un valor está mal por sí mismo, sin importar el estado del sistema. */
public abstract class InvalidValueException extends DomainException {

    protected InvalidValueException(String message) {
        super(message);
    }
}
