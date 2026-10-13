package ar.edu.itba.certiflow.certification.model;

import java.util.function.Function;
import java.util.function.Predicate;

/** Una regla que un candidato a certificar tiene que cumplir; si no la cumple, explica por qué. */
public record Requirement<T>(Predicate<T> isMet, IneligibilityReason reason, Function<T, String> detail) {

    boolean isMetBy(T candidate) {
        return isMet.test(candidate);
    }

    Ineligibility ineligibilityOf(T candidate) {
        return new Ineligibility(reason, detail.apply(candidate));
    }
}
