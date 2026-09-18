package ar.edu.itba.certiflow.domain.evaluation;

/**
 * Decide si una respuesta cumple un criterio. El tipo de respuesta que una regla acepta forma parte
 * de su firma, de modo que ofrecerle una respuesta incompatible es un error de compilación.
 * Las implementaciones deben ser inmutables y deterministas: la misma respuesta produce siempre
 * el mismo resultado, porque la evaluación de una inspección se deriva cada vez que se la pide.
 */
public interface ApprovalRule<A extends Answer> {

    boolean isSatisfiedBy(A answer);
}
