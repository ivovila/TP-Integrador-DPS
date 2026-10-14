package ar.edu.itba.certiflow.schema.model;

/**
 * Un tipo de regla de aprobación: su nombre y cómo se arma una regla suya a partir de sus parámetros.
 * Agregar un tipo de regla es agregar la regla, su tipo y registrarlo en el catálogo.
 */
public interface ApprovalRuleKind {

    boolean isNamed(String ruleKindName);

    ApprovalRule<?> ruleFrom(RuleParameters parameters);
}
