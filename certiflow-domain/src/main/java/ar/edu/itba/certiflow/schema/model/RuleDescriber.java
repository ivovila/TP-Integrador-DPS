package ar.edu.itba.certiflow.schema.model;

import java.math.BigDecimal;
import java.util.Set;

/**
 * Quien necesita escribir una regla (para guardarla o publicarla) recibe su tipo y sus parámetros con nombre.
 * Los métodos son por clase de valor y no por tipo de regla: una regla nueva usa estos mismos, así que
 * escribirla no obliga a tocar esta interfaz ni a quienes la implementan.
 */
public interface RuleDescriber<T> {

    RuleDescriber<T> text(String parameterName, String value);

    RuleDescriber<T> decimal(String parameterName, BigDecimal value);

    RuleDescriber<T> texts(String parameterName, Set<String> values);

    T ofKind(String ruleKindName);
}
