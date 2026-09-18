package ar.edu.itba.certiflow.domain.shared;

import java.util.Objects;

/**
 * Una misma persona puede ser inspector, responsable, verificador o emisor: el rol lo expresa
 * el nombre del campo que la referencia, no su tipo.
 */
public record Person(String name) {

    public Person {
        Objects.requireNonNull(name, "name");
        if (name.isBlank()) {
            throw new IllegalArgumentException("A person needs a name");
        }
    }
}
