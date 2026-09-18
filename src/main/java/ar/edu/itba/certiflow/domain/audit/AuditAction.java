package ar.edu.itba.certiflow.domain.audit;

/**
 * Enum extensible: cada agregado auditado aporta su propio enum de acciones,
 * de modo que sumar un agregado no modifica un enum central.
 */
public interface AuditAction {

    String label();
}
