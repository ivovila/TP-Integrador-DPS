package ar.edu.itba.certiflow.domain.evaluation;

/**
 * Enum extensible: cada entidad certificadora puede definir su propia escala implementando
 * esta interfaz, sin modificar el dominio.
 */
public interface Severity {

    String label();

    Outcome outcomeWhenUnmet();

    boolean blocksCertification();
}
