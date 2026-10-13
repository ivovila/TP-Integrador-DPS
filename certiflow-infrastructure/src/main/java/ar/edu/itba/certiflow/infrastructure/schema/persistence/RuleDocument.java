package ar.edu.itba.certiflow.infrastructure.schema.persistence;

import java.math.BigDecimal;
import java.util.Set;

/**
 * Una regla como está guardada: un único objeto JSON para todos los tipos, con vacíos los campos que
 * no son del suyo. Se mantiene así para que lo guardado y lo que se guarde de ahora en más tengan la misma forma.
 */
public record RuleDocument(String type, String unit, BigDecimal minimum, BigDecimal maximum, String expected,
                    Set<String> options) {
}
