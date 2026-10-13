package ar.edu.itba.certiflow.shared.model;

/** El estado actual de lo que ya existe no permite la operación. */
public abstract class RuleViolationException extends DomainException {

    protected RuleViolationException(String message) {
        super(message);
    }
}
