package ar.edu.itba.certiflow.certification.model;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

/** Una regla que un candidato a certificar tiene que cumplir; si no la cumple, explica por qué. */
public interface Requirement<T> {

    Optional<Ineligibility> unmetBy(T candidate);

    static <T> Requirement<T> that(Predicate<T> isMet, IneligibilityReason reason, Function<T, String> detail) {
        return candidate -> Optional.of(candidate)
                .filter(isMet.negate())
                .map(unmet -> new Ineligibility(reason, detail.apply(unmet)));
    }

    static <T> List<Ineligibility> unmetBy(Collection<Requirement<T>> requirements, T candidate) {
        return requirements.stream().flatMap(requirement -> requirement.unmetBy(candidate).stream()).toList();
    }
}
