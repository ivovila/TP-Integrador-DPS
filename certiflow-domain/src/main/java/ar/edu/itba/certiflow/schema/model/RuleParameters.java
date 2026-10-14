package ar.edu.itba.certiflow.schema.model;

import java.math.BigDecimal;
import java.util.Set;

/**
 * Los parámetros con nombre de una regla guardada o recibida. Quien los trae (la persistencia, la API) decide
 * qué significa que falte uno; el tipo de regla solo pide los suyos.
 */
public interface RuleParameters {

    String text(String parameterName);

    BigDecimal decimal(String parameterName);

    Set<String> texts(String parameterName);
}
