package ar.edu.itba.certiflow.shared.model;

public interface AuditAction {

    /** Identificador estable con el que se guarda: distingue acciones de igual nombre en distintos contextos. */
    String code();

    String name();

    String label();
}
