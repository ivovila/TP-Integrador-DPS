package ar.edu.itba.certiflow.shared.model;

import java.util.function.Supplier;

/**
 * Una unidad de trabajo atómica, cuando el caso de uso necesita elegir dónde empieza y dónde termina:
 * lo que un caso de uso necesita para ser correcto se lee en el caso de uso (D27).
 */
public interface Transactions {

    <T> T within(Supplier<T> work);
}
